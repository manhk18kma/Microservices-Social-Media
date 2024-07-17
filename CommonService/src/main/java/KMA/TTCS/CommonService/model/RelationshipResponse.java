package KMA.TTCS.CommonService.model;

import KMA.TTCS.CommonService.enumType.RelationshipType;

public class RelationshipResponse {
    String idProfile;
    String fullName;
    String urlProfilePicture;
    RelationshipType type;

    public RelationshipResponse(String idProfile, String fullName, String urlProfilePicture, RelationshipType type) {
        this.idProfile = idProfile;
        this.fullName = fullName;
        this.urlProfilePicture = urlProfilePicture;
        this.type = type;
    }

    public String getIdProfile() {
        return idProfile;
    }

    public void setIdProfile(String idProfile) {
        this.idProfile = idProfile;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUrlProfilePicture() {
        return urlProfilePicture;
    }

    public void setUrlProfilePicture(String urlProfilePicture) {
        this.urlProfilePicture = urlProfilePicture;
    }

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }
}
