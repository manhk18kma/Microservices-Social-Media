package TTCS.ProfileService.application.Query.QueryHanler;


import KMA.TTCS.CommonService.PageResponseCom;
import KMA.TTCS.CommonService.enumType.RelationshipType;
import KMA.TTCS.CommonService.model.*;
import KMA.TTCS.CommonService.query.*;
import TTCS.ProfileService.application.Exception.AppException.AppErrorCode;
import TTCS.ProfileService.application.Exception.AppException.AppException;
import TTCS.ProfileService.application.Query.Query.*;
import TTCS.ProfileService.domain.model.Follow;
import TTCS.ProfileService.domain.model.Friend;
import TTCS.ProfileService.domain.model.Profile;
import TTCS.ProfileService.infranstructure.persistence.FollowRepository;
import TTCS.ProfileService.infranstructure.persistence.FriendRepository;
import TTCS.ProfileService.infranstructure.persistence.ProfileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryExecutionException;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileQueryHandler {
    final ProfileRepository profileRepository;
    final FriendRepository friendRepository;
    final FollowRepository followRepository;

    @QueryHandler
    public List<Profile> handle(ProfileQueryGetAll profileQueryGetAll) {
        int pageNo = profileQueryGetAll.getPageNo();
        int pageSize = profileQueryGetAll.getPageSize();
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Profile> page = profileRepository.findAll(pageable);
        return page.getContent();
    }
    @QueryHandler
    public List<Friend> handle(ProfileQueryGetAllFriend queryGetAllFriend) {
        System.out.println("ProfileQueryGetAllFriend QueryHandler");
        int pageNo = queryGetAllFriend.getPageNo();
        int pageSize = queryGetAllFriend.getPageSize();
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Friend> page = friendRepository.findByIdProfile1OrIdProfile2(
                queryGetAllFriend.getIdProfile(),
                queryGetAllFriend.getIdProfile(),
                pageable
        );
        return page.getContent();
    }

    @QueryHandler
    public List<Profile> handle(ProfileQueryGetAllFollowings queryGetAllFollowings) {
        Profile profile = profileRepository.findById(queryGetAllFollowings.getIdProfile()).orElseThrow(
                ()->new AppException(AppErrorCode.ACCOUNT_NOT_EXISTED)
        );
        int pageNo = queryGetAllFollowings.getPageNo();
        int pageSize = queryGetAllFollowings.getPageSize();
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Follow> page = followRepository.findByIdProfileFollower(
                queryGetAllFollowings.getIdProfile(),
                pageable
        );
        List<Profile> profiles = page.getContent().stream().map(follow -> {
            String idProfileTarget = follow.getIdProfileTarget();
            return profileRepository.findById(idProfileTarget).get();
        }).collect(Collectors.toList());
        return profiles;
    }


    @QueryHandler
    public List<Profile> handle(ProfileQueryGetAllFollowers queryGetAllFollowers) {
        Profile profile = profileRepository.findById(queryGetAllFollowers.getIdProfile()).orElseThrow(
                ()->new AppException(AppErrorCode.ACCOUNT_NOT_EXISTED)
        );
        int pageNo = queryGetAllFollowers.getPageNo();
        int pageSize = queryGetAllFollowers.getPageSize();
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Follow> page = followRepository.findByIdProfileTarget(
                queryGetAllFollowers.getIdProfile(),
                pageable
        );
        List<Profile> profiles = page.getContent().stream().map(follow -> {
            String idProfileTarget = follow.getIdProfileFollower();
            return profileRepository.findById(idProfileTarget).get();
        }).collect(Collectors.toList());
        return profiles;    }




    @QueryHandler
    public Optional<Profile> handle(ProfileQueryGetById profileQueryGetById) {
        return profileRepository.findById(profileQueryGetById.getId());
    }


    @QueryHandler
    public ProfileMessageResponse handle(ProfileMessageQuery query) throws QueryExecutionException {

        Optional<Profile> optionalProfile = profileRepository.findById(query.getIdProfile());
        if(!optionalProfile.isPresent()){
            return null;
        }

        Profile profileTarget = optionalProfile.get();
        Set<String> list = new HashSet<>();
       if(query.getPageNo() > 0 && query.getPageSize() > 0){
           Pageable pageable = PageRequest.of(query.getPageNo(), query.getPageSize());
           Page<Friend> page = friendRepository.findByIdProfile1OrIdProfile2(
                   query.getIdProfile(),
                   query.getIdProfile(),
                   pageable
           );
            list = page.getContent().stream()
                   .map(friend -> {
                       String idTarget = query.getIdProfile().equals(friend.getIdProfile1()) ?
                               friend.getIdProfile2() :
                               friend.getIdProfile1();
                       Profile profile = profileRepository.findById(idTarget).orElse(null);
                       if (profile != null) {
                           return profile.getIdProfile();
                       }
                       return null;
                   })
                   .filter(response -> response != null)
                   .collect(Collectors.toSet());

       }else if(query.getPageNo() == 0 && query.getPageSize() == 0) {
           List<Friend> listFriends = friendRepository.findByIdProfile1OrIdProfile2(
                   query.getIdProfile(),
                   query.getIdProfile()
           );
            list = listFriends.stream()
                   .map(friend -> {
                       String idTarget = query.getIdProfile().equals(friend.getIdProfile1()) ?
                               friend.getIdProfile2() :
                               friend.getIdProfile1();
                       Profile profile = profileRepository.findById(idTarget).orElse(null);
                       if (profile != null) {
                           return profile.getIdProfile();
                       }
                       return null;
                   })
                   .filter(response -> response != null)
                    .collect(Collectors.toSet());

       }else if(query.getPageNo() == -1 && query.getPageSize() == -1){
           return new ProfileMessageResponse(profileTarget.getIdProfile(),
                   profileTarget.getFullName() ,
                   profileTarget.getUrlProfilePicture(), null);
       }


        return new ProfileMessageResponse(profileTarget.getIdProfile(),
                profileTarget.getFullName() ,
                profileTarget.getUrlProfilePicture() ,
                list);
    }


    @QueryHandler
    public PageResponseCom handle(FriendsQuery query) {
        Pageable pageable = PageRequest.of(query.getPageNo(), query.getPageSize());
        Page<Friend> page = friendRepository.findByIdProfile1OrIdProfile2(
                query.getIdProfile(),
                query.getIdProfile(),
                pageable
        );
        System.out.println("here");

        List<FriendsResponseCom> list = page.getContent().stream()
                .map(friend -> {
                    String idTarget = query.getIdProfile().equals(friend.getIdProfile1()) ?
                            friend.getIdProfile2() :
                            friend.getIdProfile1();
                    Profile profile = profileRepository.findById(idTarget).orElse(null);
                    if (profile != null) {
                        return new FriendsResponseCom(
                                profile.getIdProfile(),
                                profile.getFullName(),
                                profile.getUrlProfilePicture()
                        );
                    }
                    return null;
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());

        return new PageResponseCom<>(
                query.getPageSize(),
                (int) page.getTotalElements(),
                page.getTotalPages(),
                query.getPageNo(),
                list
        );
    }


    @QueryHandler
    public ProfileNotificationResponse handle(ProfileNotificationQuery query) {
        Profile profile = profileRepository.findById(query.getIdProfile())
                .orElseThrow(() -> new AppException(AppErrorCode.PROFILE_NOT_EXISTED));
        return new ProfileNotificationResponse(profile.getFullName() , profile.getUrlProfilePicture());
    }

    @QueryHandler
    public List<FriendsOrFollowingResponse> handle(FriendsOrFollowingQuery query) {
        Profile profile = profileRepository.findById(query.getIdProfile())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        List<String> idProfiles = new ArrayList<>();

        profile.getFollowing().forEach(follow -> idProfiles.add(follow.getIdProfileTarget()));

        profile.getFriendShip().forEach(friend -> {
            String idProfileTarget = friend.getIdProfile1().equals(query.getIdProfile())
                    ? friend.getIdProfile2()
                    : friend.getIdProfile1();
            idProfiles.add(idProfileTarget);
        });

        List<Profile> profiles = profileRepository.findAllById(idProfiles);

        List<FriendsOrFollowingResponse> responses = profiles.stream().map(profileTarget ->
                new FriendsOrFollowingResponse(
                        profileTarget.getIdProfile(),
                        profileTarget.getFullName(),
                        profileTarget.getUrlProfilePicture()
                )
        ).collect(Collectors.toList());

        return responses;
    }

    @QueryHandler
    public List<FriendsOrFollowerResponse> handle(FriendsOrFollowerQuery query) {
        Profile profile = profileRepository.findById(query.getIdProfile())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        List<String> idProfiles = new ArrayList<>();

        profile.getFollower().forEach(follow -> idProfiles.add(follow.getIdProfileFollower()));

        profile.getFriendShip().forEach(friend -> {
            String idProfileTarget = friend.getIdProfile1().equals(query.getIdProfile())
                    ? friend.getIdProfile2()
                    : friend.getIdProfile1();
            idProfiles.add(idProfileTarget);
        });

        List<Profile> profiles = profileRepository.findAllById(idProfiles);

        List<FriendsOrFollowerResponse> responses = profiles.stream().map(profileTarget ->
                new FriendsOrFollowerResponse(
                        profileTarget.getIdProfile(),
                        profileTarget.getFullName(),
                        profileTarget.getUrlProfilePicture()
                )
        ).collect(Collectors.toList());

        return responses;
    }

    @QueryHandler
    public RelationshipResponse handle(RelationshipQuery query) {
        Profile profile = profileRepository.findById(query.getIdProfile())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        Profile profileTarget = profileRepository.findById(query.getGetIdProfileTarget())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        RelationshipType type = query.getIdProfile().equals(query.getIdProfileTarget)?
                RelationshipType.YOU : RelationshipType.NONE;



        boolean isFollowing = followRepository.existsByIdProfileTargetAndIdProfileFollower(
                query.getIdProfileTarget,
                query.getIdProfile()
        );
        if(isFollowing){
            type = RelationshipType.FOLLOWING;
        }else {
            boolean isFollower = followRepository.existsByIdProfileFollowerAndIdProfileTarget(
                    query.getIdProfileTarget,
                    query.getIdProfile()
            );
            if(isFollower){
                type=RelationshipType.FOLLOWER;
            }
            else {
                boolean isFiend1 = friendRepository.existsByIdProfile1AndIdProfile2(
                        query.getIdProfileTarget,query.getIdProfile()
                );
                boolean isFiend2 = friendRepository.existsByIdProfile1AndIdProfile2(
                        query.getIdProfile() , query.getIdProfileTarget
                );
                if(isFiend1 || isFiend2){
                    type = RelationshipType.FRIEND;
                }
            }
        }

//        boolean isFollower = profile.getFollower().stream()
//                .anyMatch(follow -> follow.getIdProfileFollower().equals(query.getIdProfileTarget));
//        if (isFollower) {
//            type = RelationshipType.FOLLOWER;
//            return new RelationshipResponse(
//                    profileTarget.getIdProfile(),
//                    profileTarget.getFullName(),
//                    profileTarget.getUrlProfilePicture(),
//                    type
//            );
//        }
//
//        boolean isFollowing = profile.getFollowing().stream()
//                .anyMatch(follow -> follow.getIdProfileTarget().equals(query.getIdProfileTarget));
//        if (isFollowing) {
//            type = RelationshipType.FOLLOWING;
//            return new RelationshipResponse(
//                    profileTarget.getIdProfile(),
//                    profileTarget.getFullName(),
//                    profileTarget.getUrlProfilePicture(),
//                    type
//            );        }
//
//        boolean isFriend = profile.getFriendShip().stream()
//                .anyMatch(friend -> {
//                    String idTarget = friend.getIdProfile1().equals(profile.getIdProfile()) ?
//                            friend.getIdProfile2() : friend.getIdProfile1();
//                    return idTarget.equals(query.getIdProfileTarget);
//                });
//        if (isFriend) {
//            type = RelationshipType.FRIEND;
//            return new RelationshipResponse(
//                    profileTarget.getIdProfile(),
//                    profileTarget.getFullName(),
//                    profileTarget.getUrlProfilePicture(),
//                    type
//            );        }
        return new RelationshipResponse(
                profileTarget.getIdProfile(),
                profileTarget.getFullName(),
                profileTarget.getUrlProfilePicture(),
                type
        );


    }
}
