package net.theforeveriris.irislineconsole.dataStore.persistent;

import java.util.ArrayList;
import java.util.List;

public class ClassEntry {
    public int id = -1;
    public String name = "";
    public String display_name = "";
    public List<String> appPackageNames = new ArrayList<>();

    public ClassEntry() {
    }

    public ClassEntry(String name, String display_name) {
        this.name = name;
        this.display_name = display_name;
    }
}
