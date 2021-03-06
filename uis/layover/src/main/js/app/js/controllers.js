/*
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

/* Controllers */

var aproxControllers = angular.module('aprox.controllers', []);

aproxControllers.controller('DisplayHtmlCtl', ['$scope', '$element', function($scope, $element) {
  console.log($element);
  $scope.showHtml = function() {
      alert($element.html());
  };
}]);

aproxControllers.controller('NavCtl', ['$scope', function($scope){
  $scope.addon_navs = [];
  if ( addons !== undefined ){
    addons.items.each(function(addon){
      if ( addon.sections !== undefined ){
        addon.sections.each(function(section){
          $scope.addon_navs.push(section);
        });
      }
    });
  }
}]);

aproxControllers.controller('RemoteListCtl', ['$scope', '$location', 'RemoteSvc', 'StoreUtilSvc', 'ControlSvc', function($scope, $location, RemoteSvc, StoreUtilSvc, ControlSvc) {
    ControlSvc.addListingControlHrefs($scope, $location);
  
    $scope.listing = RemoteSvc.resource.query({}, function(listing){
      for(var i=0; i<listing.items.length; i++){
        var item = listing.items[i];
        item.detailHref = StoreUtilSvc.detailHref(item.key);
        item.storeHref = StoreUtilSvc.storeHref(item.key);
        item.name = StoreUtilSvc.nameFromKey(item.key);
        item.description = StoreUtilSvc.defaultDescription(item.description);
      }
    });
    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('RemoteCtl', ['$scope', '$routeParams', '$location', 'RemoteSvc', 'StoreUtilSvc', 'ControlSvc', function($scope, $routeParams, $location, RemoteSvc, StoreUtilSvc, ControlSvc) {
  $scope.mode = StoreUtilSvc.resourceMode();
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    timeout_seconds: '',
    cache_timeout_seconds: '',
  };
  
  $scope.controls = function( store ){
    $scope.store = store;
    
    ControlSvc.addControlHrefs($scope, 'remote', $scope.raw.name, $scope.mode, $location);
    ControlSvc.addStoreControls($scope, $location, 'remote', RemoteSvc, StoreUtilSvc, {
      save: function(scope){
        if ( scope.is_passthrough ){
          delete scope.store.cache_timeout_seconds;
        }
        else{
          scope.store.cache_timeout_seconds = StoreUtilSvc.durationToSeconds(scope.raw.cache_timeout_seconds);
        }
    
        scope.store.timeout_seconds = StoreUtilSvc.durationToSeconds(scope.raw.timeout_seconds);
      },
    });
  };

  if ( $scope.mode == 'edit' ){
    RemoteSvc.resource.get({name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.cache_timeout_seconds = StoreUtilSvc.secondsToDuration(store.cache_timeout_seconds);
      $scope.raw.timeout_seconds = StoreUtilSvc.secondsToDuration(store.timeout_seconds);

      var useX509 = store.server_certificate_pem !== undefined;
      useX509 = store.key_certificate_pem !== undefined || useX509;

      $scope.raw.use_x509 = useX509;

      var useProxy = store.proxy_host !== undefined;
      $scope.raw.use_proxy = useProxy;

      var useAuth = useProxy && store.proxy_user !== undefined;
      useAuth = store.user !== undefined || useAuth;

      $scope.raw.use_auth = useAuth;
      
      $scope.controls( store );
    });
  }
  else if ($scope.mode == 'new'){
    $scope.store = {
      url: '',
      timeout_seconds: 60,
      cache_timeout_seconds: 86400,
      is_passthrough: false
    };
    
    $scope.controls( $scope.store );
  }
  else{
    RemoteSvc.resource.get({name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.description = StoreUtilSvc.defaultDescription(store.description);

      var useX509 = store.server_certificate_pem !== undefined;
      useX509 = store.key_certificate_pem !== undefined || useX509;

      $scope.raw.use_x509 = useX509;

      var useProxy = store.proxy_host !== undefined;
      $scope.raw.use_proxy = useProxy;

      var useAuth = (useProxy && store.proxy_user !== undefined);
      useAuth = store.user !== undefined || useAuth;

      $scope.raw.use_auth = useAuth;
      
      $scope.controls( store );
    });
  }

}]);

aproxControllers.controller('HostedListCtl', ['$scope', '$location', 'HostedSvc', 'StoreUtilSvc', 'ControlSvc', function($scope, $location, HostedSvc, StoreUtilSvc, ControlSvc) {
    ControlSvc.addListingControlHrefs($scope, $location);
    
    $scope.hostedOptionLegend = StoreUtilSvc.hostedOptionLegend();
  
    $scope.listing = HostedSvc.resource.query({}, function(listing){
      for(var i=0; i<listing.items.length; i++){
        var item = listing.items[i];
        item.detailHref = StoreUtilSvc.detailHref(item.key);
        item.storeHref = StoreUtilSvc.storeHref(item.key);
        item.name = StoreUtilSvc.nameFromKey(item.key);
        item.hostedOptions = StoreUtilSvc.hostedOptions(item);
        item.description = StoreUtilSvc.defaultDescription(item.description);
      }
    });

    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('HostedCtl', ['$scope', '$routeParams', '$location', 'HostedSvc', 'StoreUtilSvc', 'ControlSvc', function($scope, $routeParams, $location, HostedSvc, StoreUtilSvc, ControlSvc) {
  $scope.mode = StoreUtilSvc.resourceMode();
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    snapshot_timeout_seconds: '',
  };
  
  $scope.showSnapshotTimeout = function(){
    return $scope.store.allow_snapshots;
  };
  
  $scope.allowUploads = function(){
    return $scope.store.allow_snapshots || $scope.store.allow_releases;
  };
  
  $scope.controls = function( store ){
    $scope.store = store;
    
    ControlSvc.addControlHrefs($scope, 'hosted', $scope.raw.name, $scope.mode, $location);
    ControlSvc.addStoreControls($scope, $location, 'hosted', HostedSvc, StoreUtilSvc, {
      save: function(scope){
        if (!$scope.store.allow_snapshots){
          delete $scope.store.snapshotTimeoutSeconds;
        }
        else{
          $scope.store.snapshotTimeoutSeconds = StoreUtilSvc.durationToSeconds($scope.raw.snapshotTimeoutSeconds);
        }
      },
    });
  };

  if ( $scope.mode == 'edit' ){
    HostedSvc.resource.get({name: $routeParams.name}, function(store){
      $scope.raw.name = $scope.storeUtils.nameFromKey(store.key);
      $scope.raw.snapshotTimeoutSeconds = StoreUtilSvc.secondsToDuration(store.snapshotTimeoutSeconds);
      
      
      $scope.controls( store );
    });
  }
  else if($scope.mode == 'new'){
    $scope.store = {
      allow_releases: true,
      allow_snapshots: true,
      snapshotTimeoutSeconds: 86400,
    };
    
    $scope.controls( $scope.store );
  }
  else{
    HostedSvc.resource.get({name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.description = StoreUtilSvc.defaultDescription(store.description);
      
      $scope.controls(store);
    });
  }
}]);

aproxControllers.controller('GroupListCtl', ['$scope', '$location', 'GroupSvc', 'StoreUtilSvc', 'ControlSvc', function($scope, $location, GroupSvc, StoreUtilSvc, ControlSvc) {
    ControlSvc.addListingControlHrefs($scope, $location);
    
    $scope.listing = GroupSvc.resource.query({}, function(listing){
      for(var i=0; i<listing.items.length; i++){
        var item = listing.items[i];
        item.detailHref = StoreUtilSvc.detailHref(item.key);
        item.storeHref = StoreUtilSvc.storeHref(item.key);
        item.type = StoreUtilSvc.typeFromKey( item.key );
        item.name = StoreUtilSvc.nameFromKey(item.key);
        item.description = StoreUtilSvc.defaultDescription(item.description);
        
        item.display = false;
        
        var oldConstituents = item.constituents;
        item.constituents = [oldConstituents.length];
        for( var j=0; j<oldConstituents.length; j++ ){
          var key = oldConstituents[j];
          var c = {
              key: oldConstituents[j],
              detailHref: StoreUtilSvc.detailHref(key),
              storeHref: StoreUtilSvc.storeHref(key),
              type: StoreUtilSvc.typeFromKey( key ),
              name: StoreUtilSvc.nameFromKey(key),
          }
          item.constituents[j] = c;
        }
      }
    });
    
    $scope.displayConstituents = function(item){
      item.display = true;
    };
    
    $scope.hideConstituents = function(item){
      item.display = false;
    };
    
    $scope.hideAll = function(){
      for(var i=0; i<$scope.listing.items.length; i++){
        var item = $scope.listing.items[i];
        $scope.hideConstituents(item);
      }
    }

    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('GroupCtl', ['$scope', '$routeParams', '$location', 'GroupSvc', 'StoreUtilSvc', 'ControlSvc', 'AllEndpointsSvc', function($scope, $routeParams, $location, GroupSvc, StoreUtilSvc, ControlSvc, AllEndpointsSvc) {
  $scope.mode = StoreUtilSvc.resourceMode();
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    available: [],
    constituentHrefs: {},
  };
  
  $scope.controls = function(store){
    $scope.store = store;
    
    AllEndpointsSvc.resource.query(function(listing){
      $scope.raw.available = StoreUtilSvc.sortEndpoints( listing.items );
    });

    ControlSvc.addControlHrefs($scope, 'group', $scope.raw.name, $scope.mode, $location);
    ControlSvc.addStoreControls($scope, $location, 'group', GroupSvc, StoreUtilSvc);
  };

  if ( $scope.mode == 'edit' ){
    GroupSvc.resource.get({name: $routeParams.name}, function(store){
      $scope.raw.name = $scope.storeUtils.nameFromKey(store.key);
      
      if ( !store.constituents ){
        store.constituents = [];
      }
      
      $scope.controls(store);
    });
  }
  else if ($scope.mode == 'new'){
    $scope.store = {
      constituents: [],
    };
    
    $scope.controls($scope.store);
  }
  else{
    GroupSvc.resource.get({name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.description = StoreUtilSvc.defaultDescription(store.description);

      for(var i=0; i<store.constituents.length; i++){
        var item = store.constituents[i];
        $scope.raw.constituentHrefs[item] = {
          detailHref: StoreUtilSvc.detailHref(item),
        };
      }
      
      $scope.controls(store);
    });
  }
}]);

aproxControllers.controller('NfcController', ['$scope', '$routeParams', '$location', 'NfcSvc', 'StoreUtilSvc', 'AllEndpointsSvc', 
                                              function($scope, $routeParams, $location, NfcSvc, StoreUtilSvc, AllEndpointsSvc){
  $scope.raw = {
    available: [],
  };
  
  $scope.clearAllNFC = function(){
    NfcSvc.resource.deleteAll();
    $location.path('/nfc');
  };
  
  $scope.clearSection = function(section){
    var key = section.key;
    
//    alert( "Clear all NFC entries for: " + key );
    
    var name=StoreUtilSvc.nameFromKey(key);
    var type = StoreUtilSvc.typeFromKey(key);
    
    NfcSvc.resource.delete({name: name, type: type},
      function(){
        $scope.message = {type: 'OK', message: 'Cleared NFC for ' + key + "'"};
//        alert( "NFC for " + key + " has been cleared!");
      }, 
      function(error){
        $scope.message = {type: 'ERROR', message: 'Failed to clear NFC for ' + key + "'", detail: error};
//        alert('[ERROR] Failed to clear NFC for ' + key + "'\n" + error );
      }
    );
    
    section.paths = [];
  };
  
  $scope.clearSectionPath = function(section, path){
    path = path.substring(1);
    var key = section.key;
    
//    alert( "Clear all NFC entries for: " + key + ", path: " + path );
    
    var name=StoreUtilSvc.nameFromKey(key);
    var type = StoreUtilSvc.typeFromKey(key);
    
    NfcSvc.resource.delete({name: name, type: type, path: path},
      function(){
        $scope.message = {type: 'OK', message: 'Cleared NFC for ' + key + "', path: " + path};
//        alert( "NFC for: " + key + ", path: " + path + " has been cleared!" );
      }, 
      function(error){
        $scope.message = {type: 'ERROR', message: 'Failed to clear NFC for ' + key + "'", detail: error};
//        alert('[ERROR] Failed to clear NFC for ' + key + "', path: " + path + "\n" + error );
      }
    );
    
    var idx = section.paths.indexOf(path);
    section.paths.splice(idx,1);
  };
  
  $scope.showAll = function(){
    if ( !window.location.hash.startsWith( "#/nfc/view/all" ) ){
      $location.path('/nfc/view/all');
    }
  };
  
  $scope.show = function(){
    if ( !$scope.currentKey ){return;}
    
    var viewPath = '/nfc/view/' + $scope.currentKey.replace(':', '/');
    
    if ( !window.location.hash.startsWith( "#" + viewPath ) ){
      $location.path(viewPath);
    }
  };
  
  AllEndpointsSvc.resource.query(function(listing){
    var available = [];
    listing.items.each(function(item){
      item.key = StoreUtilSvc.formatKey(item.type, item.name);
      item.label = StoreUtilSvc.keyLabel(item.key);
      available.push(item);
    });
    
    $scope.raw.available = StoreUtilSvc.sortEndpoints( available );
  });
  
  if ( window.location.hash == ( "#/nfc/view/all" ) ){
//    alert( "showing all NFC entries");
    delete $scope.currentKey;
    
    NfcSvc.resource.query({}, function(nfc){
      nfc.sections.each(function(section){
        section.label = StoreUtilSvc.keyLabel(section.key);
        section.paths.sort();
      });
      
      $scope.sections = StoreUtilSvc.sortByEmbeddedKey(nfc.sections);
    });
  }
  else{
    var routeType = $routeParams.type;
    var routeName = $routeParams.name;
    
    if ( routeType !== undefined && routeName !== undefined ){
      $scope.currentKey = StoreUtilSvc.formatKey(routeType, routeName);
      
//      alert( "showing NFC entries for: " + $scope.currentKey);
      
      NfcSvc.resource.get({type:routeType, name:routeName}, function(nfc){
        nfc.sections.each(function(section){
          section.label = StoreUtilSvc.keyLabel(section.key);
          section.paths.sort();
        });
        
        $scope.sections = StoreUtilSvc.sortByEmbeddedKey(nfc.sections);
      });
    }
  }
}]);

aproxControllers.controller('FooterCtl', ['$scope', 'FooterSvc', function($scope, FooterSvc){
    $scope.stats = FooterSvc.resource.query();
  }]);

