package KMA.TTCS.CommonService.query;

public class GetIdChatQuery {
    String idProfile1;
    String idProfile2;

    public GetIdChatQuery(String idProfile1, String idProfile2) {
        this.idProfile1 = idProfile1;
        this.idProfile2 = idProfile2;
    }

    public String getIdProfile1() {
        return idProfile1;
    }

    public void setIdProfile1(String idProfile1) {
        this.idProfile1 = idProfile1;
    }

    public String getIdProfile2() {
        return idProfile2;
    }

    public void setIdProfile2(String idProfile2) {
        this.idProfile2 = idProfile2;
    }
}
