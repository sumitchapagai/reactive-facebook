/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow
 * @format
 */
'use strict';

const Button = require('Button');
const DeviceInfo = require('DeviceInfo');
const Modal = require('Modal');
const React = require('react');
const SafeAreaView = require('SafeAreaView');
const StyleSheet = require('StyleSheet');
const Text = require('Text');
const View = require('View');

exports.displayName = (undefined: ?string);
exports.framework = 'React';
exports.title = '<SafeAreaView>';
exports.description =
  'SafeAreaView automatically applies paddings reflect the portion of the view that is not covered by other (special) ancestor views.';

type Insets = {left: number, top: number, right: number, bottom: number} | void;

class SafeAreaViewExample extends React.Component<
  {},
  {|
    modalVisible: boolean,
    insets: Insets,
  |},
> {
  state = {
    modalVisible: false,
    insets: undefined,
  };

  _setModalVisible = visible => {
    this.setState({modalVisible: visible});
  };

  _onSafeAreaViewInsetsChange = event => {
    this.setState({insets: event.nativeEvent.insets});
  };

  render() {
    return (
      <View>
        <Modal
          visible={this.state.modalVisible}
          onRequestClose={() => this._setModalVisible(false)}
          animationType="slide"
          supportedOrientations={['portrait', 'landscape']}>
          <View style={styles.modal}>
            <SafeAreaView
              style={styles.safeArea}
              onInsetsChange={this._onSafeAreaViewInsetsChange}>
              <View style={styles.safeAreaContent}>
                <Text>
                  {this.state.insets &&
                    `safeAreaViewInsets:\n${JSON.stringify(this.state.insets)}`}
                </Text>
                <Button
                  onPress={this._setModalVisible.bind(this, false)}
                  title="Close"
                />
              </View>
            </SafeAreaView>
          </View>
        </Modal>
        <Button
          onPress={this._setModalVisible.bind(this, true)}
          title="Present Modal Screen with SafeAreaView"
        />
      </View>
    );
  }
}

exports.examples = [
  {
    title: '<SafeAreaView> Example',
    description:
      'SafeAreaView automatically applies paddings reflect the portion of the view that is not covered by other (special) ancestor views.',
    render: () => <SafeAreaViewExample />,
  },
];

var styles = StyleSheet.create({
  modal: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
    height: 1000,
  },
  safeAreaContent: {
    flex: 1,
    backgroundColor: '#ffaaaa',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
