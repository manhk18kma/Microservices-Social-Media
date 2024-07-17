package KMA.TTCS.CommonService.query;

public class GetIdChatProfileQuery {
    private String idProfile;

    public GetIdChatProfileQuery(String idProfile) {
        this.idProfile = idProfile;
    }

    public String getIdProfile() {
        return idProfile;
    }

    public void setIdProfile(String idProfile) {
        this.idProfile = idProfile;
    }
}
