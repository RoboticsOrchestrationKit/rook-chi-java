function init() {
  send_listen_json('{ "type": "subscribe" }', handle_io_message);
}

function handle_io_message(json) {
  handle_io_value(json.name, json.value);
}

function handle_io_value(name, value) {
  var table = $("#table");
  var rowId = "value_" + name.replace(/ /g,'');
  if($("#"+rowId).length == 0) {
    template_create("template_row", rowId, table)
        .find('[name="key"]').html(name + ": ");
  }
  $("#"+rowId).find('[name="value"]').html(value);
}

function send_listen_json(text, response_handler) {
  var ws = new WebSocket("ws://" + window.location.host, "json");
  ws.onmessage = function (evt)
  {
    if(response_handler != null) {
      var json = JSON.parse(evt.data);
      response_handler(json);
    }
  };
  ws.onopen = function()
  {
    ws.send(text);
  };
}
