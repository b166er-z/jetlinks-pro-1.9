package org.jetlinks.pro.geo;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.types.GeoPoint;
import org.jetlinks.core.metadata.types.GeoShape;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class GeoObject implements Serializable {

    private static final long serialVersionUID = -6849794470754667710L;

    //唯一标识
    @NotBlank(message = "[id]不能为空")
    private String id;

    //类型,如: device
    @NotBlank(message = "[objectType]不能为空")
    private String objectType;

    //类型,如: point
    @NotBlank(message = "[shapeType]不能为空")
    private String shapeType;

    //对象标识,如: deviceId
    @NotBlank(message = "[objectId]不能为空")
    private String objectId;

    //属性标识如: location
    private String property;

    //坐标
    private GeoPoint point;

    //地形
    private GeoShape shape;

    //拓展信息
    private Map<String, Object> tags;

    //时间戳: 数据更新的时间
    private long timestamp;

    public void setPoint(GeoPoint point) {
        this.shapeType = GeoShape.Type.Point.name();
        this.point = point;
        this.shape = GeoShape.fromPoint(point);
    }

    public void setShape(GeoShape shape) {
        if (shape == null) {
            this.shape = null;
            return;
        }
        this.shapeType = shape.getType().name();
        this.shape = shape;
        if (shape.getType() == GeoShape.Type.Point && this.point == null) {
            this.point = GeoPoint.of(shape.getCoordinates());
        }
    }
}
