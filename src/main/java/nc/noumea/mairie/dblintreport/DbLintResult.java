package nc.noumea.mairie.dblintreport;

/**
 * Created by barmi83 on 22/04/16.
 */
public class DbLintResult {

    private int globalScore;

    private int nbCriticalHit;
    private int nbHighHit;
    private int nbMediumHit;
    private int nbLowHit;
    private String jsonStringHits;

    public int getGlobalScore() {
        return globalScore;
    }

    public void setGlobalScore(int globalScore) {
        this.globalScore = globalScore;
    }

    public int getNbCriticalHit() {
        return nbCriticalHit;
    }

    public void setNbCriticalHit(int nbCriticalHit) {
        this.nbCriticalHit = nbCriticalHit;
    }

    public int getNbHighHit() {
        return nbHighHit;
    }

    public void setNbHighHit(int nbHighHit) {
        this.nbHighHit = nbHighHit;
    }

    public int getNbMediumHit() {
        return nbMediumHit;
    }

    public void setNbMediumHit(int nbMediumHit) {
        this.nbMediumHit = nbMediumHit;
    }

    public int getNbLowHit() {
        return nbLowHit;
    }

    public void setNbLowHit(int nbLowHit) {
        this.nbLowHit = nbLowHit;
    }

    public String getJsonStringHits() {
        return jsonStringHits;
    }

    public void setJsonStringHits(String jsonStringHits) {
        this.jsonStringHits = jsonStringHits;
    }

}
