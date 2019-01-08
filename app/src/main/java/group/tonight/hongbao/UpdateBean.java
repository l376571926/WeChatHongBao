package group.tonight.hongbao;

import java.io.Serializable;

public class UpdateBean implements Serializable {

    /**
     * id : 5c2f2886548b7a7d78997185
     * version : 1.0.0 alpha-37
     * build : 11
     * changelog : 修复版本更新内容显示不全的bug
     * fsize : 7581932
     * created_at : 1546594438
     */

    private String id;
    private String version;
    private long build;
    private String changelog;
    private long fsize;
    private long created_at;

    private String apkUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getBuild() {
        return build;
    }

    public void setBuild(long build) {
        this.build = build;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public long getFsize() {
        return fsize;
    }

    public void setFsize(long fsize) {
        this.fsize = fsize;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }

    @Override
    public String toString() {
        return "UpdateBean{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", build=" + build +
                ", changelog='" + changelog + '\'' +
                ", fsize=" + fsize +
                ", created_at=" + created_at +
                ", apkUrl='" + apkUrl + '\'' +
                '}';
    }
}
