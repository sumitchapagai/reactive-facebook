/**
 * The examples provided by Facebook are for non-commercial testing and
 * evaluation purposes only.
 *
 * Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @flow
 */
'use strict';

var React = require('react-native');
var {
  Text,
  View,
} = React;
var QuickActionsIOS = require("QuickActionsIOS");

var QUICK_ACTIONS = [
  {
    type: 'QuickActionsExample',
    title: 'Quick Actions Example'
  },
  {
    type: 'ActionSheetExample',
    title: 'Action Sheet Example',
    subtitle: 'An another example'
  }
]

var QuickActionsExample = React.createClass({
  componentWillMount() {
    QuickActionsIOS.setQuickActionsWithActionList(QUICK_ACTIONS);
  },

  render() {
    return (
      <View>
        <Text>
          Return to your home screen and force on your app icon (6s only).
        </Text>
      </View>
    );
  }
});

exports.title = 'QuickActionsIOS';
exports.description = 'Dynamic 3d touch Quick Actions';
exports.examples = [
  {
    title: 'Quick Actions',
    render(): ReactElement { return <QuickActionsExample />; }
  }
];
