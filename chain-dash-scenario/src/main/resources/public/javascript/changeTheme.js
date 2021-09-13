/* --------------------------------------------------------
 Template Settings
 -----------------------------------------------------------*/
$(function(){
    var settings = '<a id="settings" href="#changeSkin" data-toggle="modal">' +
        '<i class="fa fa-gear"></i> Change Skin' +
        '</a>' +
        '<div class="modal fade" id="changeSkin" tabindex="-1" role="dialog" aria-hidden="true">' +
        '<div class="modal-dialog modal-lg">' +
        '<div class="modal-content">' +
        '<div class="modal-header">' +
        '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>' +
        '<h4 class="modal-title">Change Template Skin</h4>' +
        '</div>' +
        '<div class="modal-body">' +
        '<div class="row template-skins">' +
        '<a data-skin="skin-blur-violate" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/violate.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-lights" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/lights.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-city" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/city.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-greenish" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/greenish.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-night" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/night.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-blue" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/blue.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-sunny" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/sunny.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-cloth" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/cloth.png" alt="">' +
        '</a>' +
        '<a data-skin="skin-tectile" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/tectile.png" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-chrome" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/chrome.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-ocean" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/ocean.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-sunset" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/sunset.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-yellow" class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/yellow.jpg" alt="">' +
        '</a>' +
        '<a data-skin="skin-blur-kiwi"class="col-sm-2 col-xs-4" href="">' +
        '<img src="/img/body/kiwi.jpg" alt="">' +
        '</a>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>';
    $('#themeSetting').prepend(settings);

    $('body').on('click', '.template-skins > a', function(e){
        e.preventDefault();
        var skin = $(this).attr('data-skin');
        $('body').attr('id', skin);
        $('#changeSkin').modal('hide');
    });
});