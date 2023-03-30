package org.jetlinks.pro.device.measurements;

import org.jetlinks.pro.dashboard.Dashboard;
import org.jetlinks.pro.dashboard.DashboardDefinition;

public interface DeviceDashboard extends Dashboard {


    @Override
    default DashboardDefinition getDefinition() {
        return DeviceDashboardDefinition.instance;
    }
}
