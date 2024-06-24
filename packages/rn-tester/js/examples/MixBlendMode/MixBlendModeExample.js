/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow strict-local
 * @format
 * @oncall react_native
 */

import type {RNTesterModuleExample} from '../../types/RNTesterTypes';
import type {ViewStyleProp} from 'react-native/Libraries/StyleSheet/StyleSheet';

import React from 'react';
import {Image, ImageBackground, StyleSheet, View} from 'react-native';

type Props = $ReadOnly<{
  style: ViewStyleProp,
  testID?: string,
}>;

function LayeredView(props: Props) {
  return (
    <>
      <View style={styles.container}>
        <ImageBackground
          source={require('../../assets/rainbow.jpeg')}
          style={styles.commonBackDrop}>
          <Image
            source={require('../../assets/alpha-hotdog.png')}
            style={[styles.commonImage, props.style]}
          />
        </ImageBackground>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  commonImage: {
    width: 200,
    height: 90,
    marginTop: 10,
  },
  container: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  commonBackDrop: {
    width: 200,
    height: 110,
  },
});

const mixBlendModes = [
  'normal',
  'multiply',
  'screen',
  'overlay',
  'darken',
  'lighten',
  'color-dodge',
  'color-burn',
  'hard-light',
  'soft-light',
  'difference',
  'exclusion',
  'hue',
  'saturation',
  'color',
  'luminosity',
];

const examples: Array<RNTesterModuleExample> = mixBlendModes.map(mode => ({
  title: mode,
  description: `mix-blend-mode: ${mode}`,
  name: mode,
  render(): React.Node {
    return <LayeredView style={{experimental_mixBlendMode: mode}} />;
  },
}));

exports.title = 'MixBlendMode';
exports.category = 'UI';
exports.description =
  'A set of graphical effects that can be applied to a view.';
exports.examples = examples;
