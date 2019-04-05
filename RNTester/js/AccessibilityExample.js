/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

const React = require('react');
const ReactNative = require('react-native');
const {
  AccessibilityInfo,
  Button,
  Text,
  View,
  TouchableOpacity,
  Alert,
  UIManager,
  findNodeHandle,
  Platform,
} = ReactNative;

const RNTesterBlock = require('./RNTesterBlock');

class AccessibilityExample extends React.Component {
  render() {
    return (
      <View>
        <RNTesterBlock title="TextView without label">
          <Text>
            Text's accessibilityLabel is the raw text itself unless it is set
            explicitly.
          </Text>
        </RNTesterBlock>

        <RNTesterBlock title="TextView with label">
          <Text accessibilityLabel="I have label, so I read it instead of embedded text.">
            This text component's accessibilityLabel is set explicitly.
          </Text>
        </RNTesterBlock>

        <RNTesterBlock title="Nonaccessible view with TextViews">
          <View>
            <Text style={{color: 'green'}}>This is text one.</Text>
            <Text style={{color: 'blue'}}>This is text two.</Text>
          </View>
        </RNTesterBlock>

        <RNTesterBlock title="Accessible view with TextViews wihout label">
          <View accessible={true}>
            <Text style={{color: 'green'}}>This is text one.</Text>
            <Text style={{color: 'blue'}}>This is text two.</Text>
          </View>
        </RNTesterBlock>

        <RNTesterBlock title="Accessible view with TextViews with label">
          <View
            accessible={true}
            accessibilityLabel="I have label, so I read it instead of embedded text.">
            <Text style={{color: 'green'}}>This is text one.</Text>
            <Text style={{color: 'blue'}}>This is text two.</Text>
          </View>
        </RNTesterBlock>

        {/* Android screen readers will say the accessibility hint instead of the text
        since the view doesn't have a label. */}
        <RNTesterBlock title="Accessible view with TextViews with hint">
          <View accessibilityHint="Accessibility hint." accessible={true}>
            <Text style={{color: 'green'}}>This is text one.</Text>
            <Text style={{color: 'blue'}}>This is text two.</Text>
          </View>
        </RNTesterBlock>

        <RNTesterBlock title="Accessible view TextViews with label and hint">
          <View
            accessibilityLabel="Accessibility label."
            accessibilityHint="Accessibility hint."
            accessible={true}>
            <Text style={{color: 'green'}}>This is text one.</Text>
            <Text style={{color: 'blue'}}>This is text two.</Text>
          </View>
        </RNTesterBlock>

        <RNTesterBlock title="Text with accessibilityRole = header">
          <Text accessibilityRole="header">This is a title.</Text>
        </RNTesterBlock>

        <RNTesterBlock title="Touchable with accessibilityRole = link">
          <TouchableOpacity
            onPress={() => Alert.alert('Link has been clicked!')}
            accessibilityRole="link">
            <View>
              <Text>Click me</Text>
            </View>
          </TouchableOpacity>
        </RNTesterBlock>

        <RNTesterBlock title="Touchable with accessibilityRole = button">
          <TouchableOpacity
            onPress={() => Alert.alert('Button has been pressed!')}
            accessibilityRole="button">
            <Text>Click me</Text>
          </TouchableOpacity>
        </RNTesterBlock>

        <RNTesterBlock title="Disabled Touchable with role">
          <TouchableOpacity
            onPress={() => Alert.alert('Button has been pressed!')}
            accessibilityRole="button"
            accessibilityStates={['disabled']}
            disabled={true}>
            <View>
              <Text>
                I am disabled. Clicking me will not trigger any action.
              </Text>
            </View>
          </TouchableOpacity>
        </RNTesterBlock>

        <RNTesterBlock title="View with multiple states">
          <View
            accessible={true}
            accessibilityStates={['selected', 'disabled']}>
            <Text>This view is selected and disabled.</Text>
          </View>
        </RNTesterBlock>

        <RNTesterBlock title="View with label, hint, role, and state">
          <View
            accessible={true}
            accessibilityLabel="Accessibility label."
            accessibilityRole="button"
            accessibilityStates={['selected']}
            accessibilityHint="Accessibility hint.">
            <Text>Accessible view with label, hint, role, and state</Text>
          </View>
        </RNTesterBlock>
      </View>
    );
  }
}

class CheckboxExample extends React.Component {
  state = {
    checkboxState: 'checked',
  };

  _onCheckboxPress = () => {
    const checkboxState =
      this.state.checkboxState === 'checked' ? 'unchecked' : 'checked';

    this.setState({
      checkboxState: checkboxState,
    });

    if (Platform.OS === 'android') {
      UIManager.sendAccessibilityEvent(
        findNodeHandle(this),
        UIManager.AccessibilityEventTypes.typeViewClicked,
      );
    }
  };

  render() {
    return (
      <TouchableOpacity
        onPress={this._onCheckboxPress}
        accessibilityLabel="element 2"
        accessibilityRole="checkbox"
        accessibilityStates={[this.state.checkboxState]}
        accessibilityHint="click me to change state">
        <Text>Checkbox example</Text>
      </TouchableOpacity>
    );
  }
}

class SwitchExample extends React.Component {
  state = {
    switchState: 'checked',
  };

  _onSwitchToggle = () => {
    const switchState =
      this.state.switchState === 'checked' ? 'unchecked' : 'checked';

    this.setState({
      switchState: switchState,
    });

    if (Platform.OS === 'android') {
      UIManager.sendAccessibilityEvent(
        findNodeHandle(this),
        UIManager.AccessibilityEventTypes.typeViewClicked,
      );
    }
  };

  render() {
    return (
      <TouchableOpacity
        onPress={this._onSwitchToggle}
        accessibilityLabel="element 12"
        accessibilityRole="switch"
        accessibilityStates={[this.state.switchState]}
        accessible={true}>
        <Text>Switch example</Text>
      </TouchableOpacity>
    );
  }
}

class SelectionExample extends React.Component {
  constructor(props) {
    super(props);
    this.selectableElement = React.createRef();
  }

  state = {
    isSelected: true,
    isEnabled: false,
  };

  render() {
    let accessibilityStates = [];
    let accessibilityHint = 'click me to select';
    if (this.state.isSelected) {
      accessibilityStates.push('selected');
      accessibilityHint = 'click me to unselect';
    }
    if (!this.state.isEnabled) {
      accessibilityStates.push('disabled');
      accessibilityHint = 'use the button on the right to enable selection';
    }
    let buttonTitle = this.state.isEnabled
      ? 'Disable selection'
      : 'Enable selection';

    return (
      <View style={{flex: 1, flexDirection: 'row'}}>
        <TouchableOpacity
          ref={this.selectableElement}
          accessible={true}
          onPress={() => {
            this.setState({
              isSelected: !this.state.isSelected,
            });

            if (Platform.OS === 'android') {
              UIManager.sendAccessibilityEvent(
                findNodeHandle(this.selectableElement.current),
                UIManager.AccessibilityEventTypes.typeViewClicked,
              );
            }
          }}
          accessibilityLabel="element 19"
          accessibilityStates={accessibilityStates}
          accessibilityHint={accessibilityHint}>
          <Text>Selectable element example</Text>
        </TouchableOpacity>
        <Button
          onPress={() => {
            this.setState({
              isEnabled: !this.state.isEnabled,
            });
          }}
          title={buttonTitle}
        />
      </View>
    );
  }
}

class ExpandableElementExample extends React.Component {
  state = {
    expandState: 'collapsed',
  };

  _onElementPress = () => {
    const expandState =
      this.state.expandState === 'collapsed' ? 'expanded' : 'collapsed';

    this.setState({
      expandState: expandState,
    });

    if (Platform.OS === 'android') {
      UIManager.sendAccessibilityEvent(
        findNodeHandle(this),
        UIManager.AccessibilityEventTypes.typeViewClicked,
      );
    }
  };

  render() {
    return (
      <TouchableOpacity
        onPress={this._onElementPress}
        accessibilityLabel="element 18"
        accessibilityStates={[this.state.expandState]}
        accessibilityHint="click me to change state">
        <Text>Expandable element example</Text>
      </TouchableOpacity>
    );
  }
}

class AccessibilityRoleAndStateExample extends React.Component<{}> {
  render() {
    return (
      <View>
        <View
          accessibilityLabel="element 1"
          accessibilityRole="alert"
          accessible={true}>
          <Text>Alert example</Text>
        </View>
        <CheckboxExample />
        <View
          accessibilityLabel="element 3"
          accessibilityRole="combobox"
          accessible={true}>
          <Text>Combobox example</Text>
        </View>
        <View
          accessibilityLabel="element 4"
          accessibilityRole="menu"
          accessible={true}>
          <Text>Menu example</Text>
        </View>
        <View
          accessibilityLabel="element 5"
          accessibilityRole="menubar"
          accessible={true}>
          <Text>Menu bar example</Text>
        </View>
        <View
          accessibilityLabel="element 6"
          accessibilityRole="menuitem"
          accessible={true}>
          <Text>Menu item example</Text>
        </View>
        <View
          accessibilityLabel="element 7"
          accessibilityRole="progressbar"
          accessible={true}>
          <Text>Progress bar example</Text>
        </View>
        <View
          accessibilityLabel="element 8"
          accessibilityRole="radio"
          accessible={true}>
          <Text>Radio button example</Text>
        </View>
        <View
          accessibilityLabel="element 9"
          accessibilityRole="radiogroup"
          accessible={true}>
          <Text>Radio group example</Text>
        </View>
        <View
          accessibilityLabel="element 10"
          accessibilityRole="scrollbar"
          accessible={true}>
          <Text>Scrollbar example</Text>
        </View>
        <View
          accessibilityLabel="element 11"
          accessibilityRole="spinbutton"
          accessible={true}>
          <Text>Spin button example</Text>
        </View>
        <SwitchExample />
        <View
          accessibilityLabel="element 13"
          accessibilityRole="tab"
          accessible={true}>
          <Text>Tab example</Text>
        </View>
        <View
          accessibilityLabel="element 14"
          accessibilityRole="tablist"
          accessible={true}>
          <Text>Tab list example</Text>
        </View>
        <View
          accessibilityLabel="element 15"
          accessibilityRole="timer"
          accessible={true}>
          <Text>Timer example</Text>
        </View>
        <View
          accessibilityLabel="element 16"
          accessibilityRole="toolbar"
          accessible={true}>
          <Text>Toolbar example</Text>
        </View>
        <View
          accessibilityLabel="element 17"
          accessibilityStates={['busy']}
          accessible={true}>
          <Text>State busy example</Text>
        </View>
        <ExpandableElementExample />
        <SelectionExample />
      </View>
    );
  }
}

class ScreenReaderStatusExample extends React.Component<{}> {
  state = {
    screenReaderEnabled: false,
  };

  componentDidMount() {
    AccessibilityInfo.addEventListener(
      'change',
      this._handleScreenReaderToggled,
    );
    AccessibilityInfo.fetch().done(isEnabled => {
      this.setState({
        screenReaderEnabled: isEnabled,
      });
    });
  }

  componentWillUnmount() {
    AccessibilityInfo.removeEventListener(
      'change',
      this._handleScreenReaderToggled,
    );
  }

  _handleScreenReaderToggled = isEnabled => {
    this.setState({
      screenReaderEnabled: isEnabled,
    });
  };

  render() {
    return (
      <View>
        <Text>
          The screen reader is{' '}
          {this.state.screenReaderEnabled ? 'enabled' : 'disabled'}.
        </Text>
      </View>
    );
  }
}

exports.title = 'Accessibility';
exports.description = 'Examples of using Accessibility APIs.';
exports.examples = [
  {
    title: 'Accessibility elements',
    render(): React.Element<typeof AccessibilityExample> {
      return <AccessibilityExample />;
    },
  },
  {
    title: 'New accessibility roles and states',
    render(): React.Element<typeof AccessibilityRoleAndStateExamples> {
      return <AccessibilityRoleAndStateExample />;
    },
  },
  {
    title: 'Check if the screen reader is enabled',
    render(): React.Element<typeof ScreenReaderStatusExample> {
      return <ScreenReaderStatusExample />;
    },
  },
];
