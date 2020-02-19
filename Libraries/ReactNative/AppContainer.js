/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 * @flow
 */

'use strict';

const EmitterSubscription = require('../vendor/emitter/EmitterSubscription');
const RCTDeviceEventEmitter = require('../EventEmitter/RCTDeviceEventEmitter');
const PropTypes = require('prop-types');
const React = require('react');
const RootTagContext = require('./RootTagContext');
const StyleSheet = require('../StyleSheet/StyleSheet');
const View = require('../Components/View/View');

type Props = $ReadOnly<{|
  children?: React.Node,
  fabric?: boolean,
  rootTag: number,
  showArchitectureIndicator?: boolean,
  WrapperComponent?: ?React.ComponentType<any>,
  internal_excludeLogBox?: ?boolean,
|}>;

type State = {|
  inspector: ?React.Node,
  mainKey: number,
  hasError: boolean,
|};

type Context = {rootTag: number, ...};

const AppContainerContext = React.createContext<Context>({rootTag: 0});

class AppContainer extends React.Component<Props, State> {
  state: State = {
    inspector: null,
    mainKey: 1,
    hasError: false,
  };
  _mainRef: ?React.ElementRef<typeof View>;
  _subscription: ?EmitterSubscription = null;

  static getDerivedStateFromError: any = undefined;

  static AppContainerContext: React$Context<Context> = AppContainerContext;

  static childContextTypes:
     | any		
     | {|rootTag: React$PropType$Primitive<number>|} = {		
     rootTag: PropTypes.number,		
   };		

    getChildContext(): Context {
     console.warn(
        'AppConntainer has been migrated to the new Context API. ' +
        'It is recommended to use AppContainerContext.Consumer to consume the context.'
      );
     return {		
       rootTag: this.props.rootTag,		
     };		
   }

  componentDidMount(): void {
    if (__DEV__) {
      if (!global.__RCTProfileIsProfiling) {
        this._subscription = RCTDeviceEventEmitter.addListener(
          'toggleElementInspector',
          () => {
            const Inspector = require('../Inspector/Inspector');
            const inspector = this.state.inspector ? null : (
              <Inspector
                isFabric={this.props.fabric === true}
                inspectedView={this._mainRef}
                onRequestRerenderApp={updateInspectedView => {
                  this.setState(
                    s => ({mainKey: s.mainKey + 1}),
                    () => updateInspectedView(this._mainRef),
                  );
                }}
              />
            );
            this.setState({inspector});
          },
        );
      }
    }
  }

  componentWillUnmount(): void {
    if (this._subscription != null) {
      this._subscription.remove();
    }
  }

  render(): React.Node {
    let yellowBox = null;
    if (__DEV__) {
      if (
        !global.__RCTProfileIsProfiling &&
        !this.props.internal_excludeLogBox
      ) {
        const YellowBox = require('../YellowBox/YellowBox');
        yellowBox = <YellowBox />;
      }
    }

    let innerView = (
      <View
        collapsable={!this.state.inspector}
        key={this.state.mainKey}
        pointerEvents="box-none"
        style={styles.appContainer}
        ref={ref => {
          this._mainRef = ref;
        }}>
        <AppContainerContext.Provider value={{rootTag: this.props.rootTag}}>
          {this.props.children}
        </AppContainerContext.Provider>
      </View>
    );

    const Wrapper = this.props.WrapperComponent;
    if (Wrapper != null) {
      innerView = (
        <Wrapper
          fabric={this.props.fabric === true}
          showArchitectureIndicator={
            this.props.showArchitectureIndicator === true
          }>
          {innerView}
        </Wrapper>
      );
    }
    return (
      <RootTagContext.Provider value={this.props.rootTag}>
        <View style={styles.appContainer} pointerEvents="box-none">
          {!this.state.hasError && innerView}
          {this.state.inspector}
          {yellowBox}
        </View>
      </RootTagContext.Provider>
    );
  }
}

const styles = StyleSheet.create({
  appContainer: {
    flex: 1,
  },
});

if (__DEV__) {
  if (!global.__RCTProfileIsProfiling) {
    const YellowBox = require('../YellowBox/YellowBox');
    YellowBox.install();
  }
}

module.exports = AppContainer;
