/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

/* eslint-disable react-native/no-inline-styles */

var BatchedBridge = require('BatchedBridge');
var React = require('React');
var RecordingModule = require('NativeModules')
  .SwipeRefreshLayoutRecordingModule;
var ScrollView = require('ScrollView');
var RefreshControl = require('RefreshControl');
var Text = require('Text');
var TouchableWithoutFeedback = require('TouchableWithoutFeedback');
var View = require('View');

class Row extends React.Component {
  state = {
    clicks: 0,
  };

  render() {
    return (
      <TouchableWithoutFeedback onPress={this._onPress}>
        <View>
          <Text>{this.state.clicks + ' clicks'}</Text>
        </View>
      </TouchableWithoutFeedback>
    );
  }

  _onPress = () => {
    this.setState({clicks: this.state.clicks + 1});
  };
}

var app = null;

class SwipeRefreshLayoutTestApp extends React.Component {
  state = {
    rows: 2,
  };

  componentDidMount() {
    app = this;
  }

  render() {
    var rows = [];
    for (var i = 0; i < this.state.rows; i++) {
      rows.push(<Row key={i} />);
    }
    return (
      <ScrollView
        style={{flex: 1}}
        refreshControl={
          <RefreshControl
            style={{flex: 1}}
            refreshing={false}
            onRefresh={() => RecordingModule.onRefresh()}
          />
        }>
        {rows}
      </ScrollView>
    );
  }
}

var SwipeRefreshLayoutTestModule = {
  SwipeRefreshLayoutTestApp,
  setRows: function(rows) {
    if (app != null) {
      app.setState({rows});
    }
  },
};

BatchedBridge.registerCallableModule(
  'SwipeRefreshLayoutTestModule',
  SwipeRefreshLayoutTestModule,
);

module.exports = SwipeRefreshLayoutTestModule;
