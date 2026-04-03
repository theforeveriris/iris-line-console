package net.theforeveriris.irislineconsole.dataStore.persistent;

public class AliasEntry {
    public int id = -1;
    public String alias = "";
    public String packageName = "";

    public AliasEntry() {
    }

    public AliasEntry(String alias, String packageName) {
        this.alias = alias;
        this.packageName = packageName;
    }
}
