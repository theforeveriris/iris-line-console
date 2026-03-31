package net.theforeveriris.irislineconsole.interfaces;

import net.theforeveriris.irislineconsole.applicationMain.MainActivity;

public interface EventLauncher {
    /**
     * launch corresponding event from activity
     * @param activity Source activity that triggers new activity
     */
    void launch(MainActivity activity);
}
