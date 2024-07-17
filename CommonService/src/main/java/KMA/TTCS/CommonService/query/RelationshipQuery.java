package KMA.TTCS.CommonService.query;

public class RelationshipQuery {
    private String idProfile;
    public String getIdProfileTarget;

    public RelationshipQuery(String idProfile, String getIdProfileTarget) {
        this.idProfile = idProfile;
        this.getIdProfileTarget = getIdProfileTarget;
    }

    public String getIdProfile() {
        return idProfile;
    }

    public void setIdProfile(String idProfile) {
        this.idProfile = idProfile;
    }

    public String getGetIdProfileTarget() {
        return getIdProfileTarget;
    }

    public void setGetIdProfileTarget(String getIdProfileTarget) {
        this.getIdProfileTarget = getIdProfileTarget;
    }

    @Override
    public String toString() {
        return "RelationshipQuery{" +
                "idProfile='" + idProfile + '\'' +
                ", getIdProfileTarget='" + getIdProfileTarget + '\'' +
                '}';
    }
}
