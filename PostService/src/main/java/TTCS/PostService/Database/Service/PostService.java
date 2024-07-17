package TTCS.PostService.Database.Service;

import KMA.TTCS.CommonService.model.FriendsOrFollowingResponse;
import KMA.TTCS.CommonService.model.ProfileMessageResponse;
import KMA.TTCS.CommonService.model.RelationshipResponse;
import KMA.TTCS.CommonService.query.FriendsOrFollowingQuery;
import KMA.TTCS.CommonService.query.ProfileMessageQuery;
import KMA.TTCS.CommonService.query.RelationshipQuery;
import TTCS.PostService.DTO.PageResponse;
import TTCS.PostService.DTO.Post.Request.CreatePostRequest;
import TTCS.PostService.DTO.Post.Request.UpdatePostRequest;
import TTCS.PostService.DTO.Post.Response.PostFriendsOrFollowingResponse;
import TTCS.PostService.DTO.Post.Response.PostResponse;
import TTCS.PostService.Database.Messaging.MessagingService;
import TTCS.PostService.Database.Repository.CommentRepository;
import TTCS.PostService.Database.Repository.ImageRepository;
import TTCS.PostService.Database.Repository.PostLikeRepository;
import TTCS.PostService.Database.Repository.PostRepository;
import TTCS.PostService.Entity.Image;
import TTCS.PostService.Entity.Post;
import TTCS.PostService.Exception.AppException.AppErrorCode;
import TTCS.PostService.Exception.AppException.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {

    ImageService imageService;
    PostRepository postRepository;
    ImageRepository imageRepository;
    UploadImageServiceImpl uploadImageService;
    QueryGateway queryGateway;
    MessagingService messagingService;
    PostLikeRepository postLikeRepository;
    CommentRepository commentRepository;

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostResponse createPost(CreatePostRequest createPostRequest, String idProfileToken) {
        ProfileMessageResponse profileMessageResponse;
            ProfileMessageQuery profileMessageQuery = new ProfileMessageQuery(idProfileToken, -1, -1);
            CompletableFuture<ProfileMessageResponse> future = queryGateway.query(profileMessageQuery, ResponseTypes.instanceOf(ProfileMessageResponse.class));
            profileMessageResponse = future.join();
        if(profileMessageResponse==null){
             throw  new AppException(AppErrorCode.PROFILE_NOT_EXISTED);
        }
        Post post = Post.builder()
                .idPost(UUID.randomUUID().toString())
                .caption(createPostRequest.getCaption())
                .updateAt(new Date())
                .idProfile(idProfileToken)
                .comments(new ArrayList<>())
                .images(new ArrayList<>())
                .likes(new ArrayList<>())
                .build();

        List<String> base64Images = createPostRequest.getBase64();
        List<String> imageUrls = new ArrayList<>();
        if(base64Images!=null){
            List<Image> images = base64Images.stream()
                    .map(base64 -> {
                        String idImage = UUID.randomUUID().toString();
                        String imageUrl = imageService.getUrl(base64, idImage);
                        return Image.builder()
                                .idImage(idImage)
                                .urlImage(imageUrl)
                                .post(post)
                                .build();
                    })
                    .collect(Collectors.toList());

            post.setImages(images);

             imageUrls = images.stream()
                    .map(Image::getUrlImage)
                    .collect(Collectors.toList());
        }

        postRepository.save(post);
        String message = "nameTarget just posted something new! Let's interact together.";
        messagingService.sendToKafka(message , idProfileToken,null , post.getIdPost(),"create_post_topic" , post.getIdPost());
        return PostResponse.builder()
                .idPost(post.getIdPost())
                .caption(post.getCaption())
                .updateAt(post.getUpdateAt())
                .idProfile(post.getIdProfile())
                .images(imageUrls)
                .fullName(profileMessageResponse.getFullName())
                .urlAvt(profileMessageResponse.getUrlProfilePicture())
                .build();
    }
    @Transactional
    @PreAuthorize("#post.idProfile == authentication.name and hasRole('USER')")
    public void deletePost(Post post) {
        messagingService.sendDataToServiceB(post.getIdPost());
        postRepository.delete(post);
    }


    @PreAuthorize("#post.idProfile == authentication.name and hasRole('USER')")
    public PostResponse updatePost(UpdatePostRequest updatePostRequest, Post post) {


        post.setUpdateAt(new Date());
        post.setCaption(updatePostRequest.getCaption());

        List<Image> oldImages = post.getImages();

        List<String> base64OrUrl = updatePostRequest.getBase64OrUrl();

        List<Image> notRemovedImages = oldImages.stream()
                .filter(image -> base64OrUrl.contains(image.getUrlImage()))
                .collect(Collectors.toList());

        List<Image> removedImages = oldImages.stream()
                .filter(image -> !base64OrUrl.contains(image.getUrlImage()))
                .collect(Collectors.toList());

        List<Image> newImages = base64OrUrl.stream()
                .filter(s -> !s.startsWith("http"))
                .map(s -> {
                    String idImage = UUID.randomUUID().toString();
                    String imageUrl = imageService.getUrl(s, idImage);
                    return Image.builder()
                            .idImage(idImage)
                            .urlImage(imageUrl)
                            .post(post)
                            .build();
                })
                .collect(Collectors.toList());

        removedImages.forEach(image -> uploadImageService.deleteImage(image.getUrlImage()));

        // Update post's images
        post.getImages().clear();
        post.getImages().addAll(notRemovedImages);
        post.getImages().addAll(newImages);

        // Save the updated post
        postRepository.save(post);
        ProfileMessageQuery profileMessageQuery = new ProfileMessageQuery(post.getIdProfile(), -1, -1);
        CompletableFuture<ProfileMessageResponse> future = queryGateway.query(profileMessageQuery, ResponseTypes.instanceOf(ProfileMessageResponse.class));
        ProfileMessageResponse profileMessageResponse = future.join();
        // Build and return the response
        return PostResponse.builder()
                .idPost(post.getIdPost())
                .caption(post.getCaption())
                .updateAt(post.getUpdateAt())
                .idProfile(post.getIdProfile())
                .images(post.getImages().stream().map(Image::getUrlImage).collect(Collectors.toList()))
                .fullName(profileMessageResponse.getFullName())
                .urlAvt(profileMessageResponse.getUrlProfilePicture())
                .build();
    }

    public Post prevCheck(String idPost){
        Post post = postRepository.findById(idPost)
                .orElseThrow(() -> new AppException(AppErrorCode.POST_NOT_EXISTED));
        return post;
    }


    public PageResponse<List<PostFriendsOrFollowingResponse>> getPostOfFriendsOrFollowing(String idProfileToken, int pageNo, int pageSize) {
        CompletableFuture<List<FriendsOrFollowingResponse>> future = queryGateway.query(
                new FriendsOrFollowingQuery(idProfileToken),
                ResponseTypes.multipleInstancesOf(FriendsOrFollowingResponse.class)
        );
        List<FriendsOrFollowingResponse> friendsOrFollowingResponses = future.join();

        List<String> idProfiles = friendsOrFollowingResponses.stream()
                .map(FriendsOrFollowingResponse::getIdProfile)
                .collect(Collectors.toList());
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Post> posts = postRepository.findByIdProfileInOrderByUpdateAtDesc(idProfiles, pageable);

        List<PostFriendsOrFollowingResponse> responses = posts.getContent().stream().map(post -> {
            Optional<FriendsOrFollowingResponse> optionalResponse = friendsOrFollowingResponses.stream()
                    .filter(f -> f.getIdProfile().equals(post.getIdProfile()))
                    .findFirst();

            String fullName = optionalResponse.map(FriendsOrFollowingResponse::getFullName).orElse(null);
            String urlAvt = optionalResponse.map(FriendsOrFollowingResponse::getUrlProfilePicture).orElse(null);

            boolean isLiked = postLikeRepository.existsByPost_IdPostAndIdProfile(post.getIdPost() ,idProfileToken);
            return PostFriendsOrFollowingResponse.builder()
                    .idPost(post.getIdPost())
                    .caption(post.getCaption())
                    .updateAt(post.getUpdateAt())
                    .images(post.getImages().stream().map(Image::getUrlImage).collect(Collectors.toList()))
                    .idProfile(post.getIdProfile())
                    .fullName(fullName)
                    .isLiked(isLiked)
                    .urlAvt(urlAvt)
                    .build();
        }).collect(Collectors.toList());

        return PageResponse.<List<PostFriendsOrFollowingResponse>>builder()
                .size(pageSize)
                .totalElements((int) posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .number(pageNo)
                .items(responses)
                .build();
    }

    public PostFriendsOrFollowingResponse getPostByID(String idPost , String idProfileToken) {
        Post post = postRepository.findById(idPost)
                .orElseThrow(() -> new AppException(AppErrorCode.POST_NOT_EXISTED));

//        ProfileMessageQuery profileMessageQuery = new ProfileMessageQuery( post.getIdProfile(), -1, -1);
//        CompletableFuture<ProfileMessageResponse> future = queryGateway.query(profileMessageQuery, ResponseTypes.instanceOf(ProfileMessageResponse.class));
//        ProfileMessageResponse profileMessageResponse = future.join();

        RelationshipQuery query = new RelationshipQuery(idProfileToken,post.getIdProfile());
        CompletableFuture<RelationshipResponse> future1 = queryGateway.query(query, ResponseTypes.instanceOf(RelationshipResponse.class));
        RelationshipResponse response = future1.join();
        boolean isLiked = postLikeRepository.existsByPost_IdPostAndIdProfile(post.getIdPost() ,idProfileToken);
        int countLikes = postLikeRepository.countAllByPost_IdPost(post.getIdPost());
        int countComments = commentRepository.countAllByPost_IdPost(post.getIdPost());
        return PostFriendsOrFollowingResponse.builder()
                .idPost(post.getIdPost())
                .caption(post.getCaption())
                .updateAt(post.getUpdateAt())
                .images(post.getImages().stream().map(Image::getUrlImage).collect(Collectors.toList()))
                .idProfile(post.getIdProfile())
                .isLiked(isLiked)
                .countComment(countComments)
                .countLikes(countLikes)
                .relationshipType(response.getType())
                .fullName(response.getFullName())
                .urlAvt(response.getUrlProfilePicture())
                .build();
    }

    public PageResponse<List<PostFriendsOrFollowingResponse>> getPostOfProfile(String idProfile, int pageNo, int pageSize, String idProfileToken) {
//        ProfileMessageQuery profileMessageQuery = new ProfileMessageQuery(idProfile, -1, -1);
//        CompletableFuture<ProfileMessageResponse> future = queryGateway.query(profileMessageQuery, ResponseTypes.instanceOf(ProfileMessageResponse.class));
//        ProfileMessageResponse profileMessageResponse = future.join();
//

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Post> posts = postRepository.findByIdProfileOrderByUpdateAtDesc(idProfile, pageable);

        RelationshipQuery query = new RelationshipQuery(idProfileToken,idProfile);
        CompletableFuture<RelationshipResponse> future1 = queryGateway.query(query, ResponseTypes.instanceOf(RelationshipResponse.class));
        RelationshipResponse response = future1.join();

        List<PostFriendsOrFollowingResponse> responses = posts.getContent().stream()
                .map(post -> {
                    int countLikes = postLikeRepository.countAllByPost_IdPost(post.getIdPost());
                    int countComments = commentRepository.countAllByPost_IdPost(post.getIdPost());
                    boolean isLiked = postLikeRepository.existsByPost_IdPostAndIdProfile(post.getIdPost(), idProfileToken);

                    return PostFriendsOrFollowingResponse.builder()
                            .idPost(post.getIdPost())
                            .caption(post.getCaption())
                            .updateAt(post.getUpdateAt())
                            .images(post.getImages().stream().map(Image::getUrlImage).collect(Collectors.toList()))
                            .idProfile(post.getIdProfile())
                            .relationshipType(response.getType())
                            .countLikes(countLikes)
                            .countComment(countComments)
                            .fullName(response.getFullName())
                            .urlAvt(response.getUrlProfilePicture())
                            .isLiked(isLiked)
                            .build();
                })
                .collect(Collectors.toList());


        return PageResponse.<List<PostFriendsOrFollowingResponse>>builder()
                .size(pageSize)
                .totalElements((int) posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .number(pageNo)
                .items(responses)
                .build();
    }

    public PageResponse<List<PostFriendsOrFollowingResponse>> getPostHomePage(String idProfileToken, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Post> posts = postRepository.findByOrderByUpdateAtDesc(pageable);

        List<PostFriendsOrFollowingResponse> responses = posts.getContent().stream().map(post -> {
            RelationshipQuery query = new RelationshipQuery(idProfileToken, post.getIdProfile());
            CompletableFuture<RelationshipResponse> future = queryGateway.query(query, ResponseTypes.instanceOf(RelationshipResponse.class));
            RelationshipResponse response = future.join();

            String fullName = response.getFullName();
            String urlAvt = response.getUrlProfilePicture();

            int countLikes = postLikeRepository.countAllByPost_IdPost(post.getIdPost());
            int countComments = commentRepository.countAllByPost_IdPost(post.getIdPost());
            boolean isLiked = postLikeRepository.existsByPost_IdPostAndIdProfile(post.getIdPost(), idProfileToken);
            return PostFriendsOrFollowingResponse.builder()
                    .idPost(post.getIdPost())
                    .caption(post.getCaption())
                    .updateAt(post.getUpdateAt())
                    .images(post.getImages().stream().map(Image::getUrlImage).collect(Collectors.toList()))
                    .idProfile(post.getIdProfile())
                    .fullName(fullName)
                    .isLiked(isLiked)
                    .countLikes(countLikes)
                    .countComment(countComments)
                    .urlAvt(urlAvt)
                    .relationshipType(response.getType())
                    .build();
        }).collect(Collectors.toList());


        return PageResponse.<List<PostFriendsOrFollowingResponse>>builder()
                .size(pageSize)
                .totalElements((int) posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .number(pageNo)
                .items(responses)
                .build();
    }

}
