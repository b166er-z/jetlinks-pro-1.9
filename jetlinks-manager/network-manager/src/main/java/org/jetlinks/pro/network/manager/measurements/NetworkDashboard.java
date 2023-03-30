package org.jetlinks.pro.network.manager.measurements;

import org.jetlinks.pro.dashboard.Dashboard;
import org.jetlinks.pro.dashboard.DashboardDefinition;

public interface NetworkDashboard extends Dashboard {

    @Override
    default DashboardDefinition getDefinition() {
        return NetworkDashboardDefinition.instance;
    }
}
