module.exports = {
  getPrinters: function(successCallback, errorCallback) {
    cordova.exec(successCallback,
        errorCallback,
        'CordovaPrinter',
        'cordovaGetPrinters');
  },
  print: function(successCallback, errorCallback, serialNumber, labelData) {
    cordova.exec(successCallback,
        errorCallback,
        'CordovaPrinter',
        'cordovaPrint', [labelData, serialNumber]);
  }
};