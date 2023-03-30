package org.jetlinks.pro.dashboard.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.pro.dashboard.MeasurementValue;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DashboardMeasurementResponse {

    private String group;

    private MeasurementValue data;


}
