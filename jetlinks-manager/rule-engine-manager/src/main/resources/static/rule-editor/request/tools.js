function getQueryVariable(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("=");
        if (pair[0] == variable) {
            return pair[1];
        }
    }
    return false;
}

function getEdgePostRequestObj(functionId, params) {
    var edgeDeviceId = getQueryVariable("edgeDeviceId");
    return {
        "url": RED.settings.apiBaseUrl + "/edge/operations/" + edgeDeviceId + "/" + functionId + "/invoke",
        "type": "POST",
        "contentType": "application/json; charset=utf-8",
        "data": JSON.stringify(params)
    };
}


var doRequest = {
    response: {},
    requestThen: null,
    /**
     * 融合网关和平台通用请求
     * @param oldObj 平台原始请求对象
     * @param functionId 融合网关请求功能id
     * @param params 融合网关请求参数
     * @param call 请求成功的回调函数
     */
    request: function (oldObj, functionId, params, call) {
        var ajaxRequestObj = oldObj;
        var _this = this;
        var edgeDeviceId = getQueryVariable("edgeDeviceId");
        if (edgeDeviceId) {
            ajaxRequestObj = getEdgePostRequestObj(functionId, params);
        }
        _this.requestThen = $.ajax(ajaxRequestObj).then(function (results) {
            _this.response = results;
            call(_this);
        });
        return _this;
    },
    successFunc: function (successCall) {
        successCall(this.response);
        return this;
    },
    resultHandler: function (call) {
        var edgeDeviceId = getQueryVariable("edgeDeviceId");
        if (edgeDeviceId) {
            this.response = call(this.response);
        }
        return this;
    }
};
