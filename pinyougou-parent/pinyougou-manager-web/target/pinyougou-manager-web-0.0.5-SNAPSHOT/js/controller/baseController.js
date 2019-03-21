app.controller('baseController',function ($scope) {
    //刷新列表
    $scope.reloadList=function () {
        $scope.search($scope.paginationConf.currentPage,$scope.paginationConf.itemsPerPage);
    }
    //分页控件配置
    $scope.paginationConf = {
        currentPage: 1,
        totalItems: 10,
        itemsPerPage: 10,
        perPageOptions: [10, 20, 30, 40, 50],
        onChange: function(){
            $scope.reloadList();//重新加载
        }
    };
    //用户勾选的id数组
    $scope.selectIds = [];
    $scope.updateSelection=function ($event,id) {
        if($event.target.checked){
            $scope.selectIds.push(id);
        }else{
            var index = $scope.selectIds.indexOf(id);
            $scope.selectIds.slice(index,1);
        }

    }
    //条件查找初始化
    $scope.searchEntity = {};

    //字符串转换成json,提取json中指定键的值

    $scope.json2String=function (jsonString,key) {
        var json = JSON.parse(jsonString);
        var value = "";
        for(var i= 0;i<json.length;i++){
            if(i > 0){
                value += ",";
            }
            value +=  json[i][key];
        }
        return value;
    }
});