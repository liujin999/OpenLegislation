var contentModule = angular.module('content');

contentModule.controller('ContentCtrl', ['$scope', function($scope) {
    $scope.selectedContent = 'bills';
}]);