/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow strict
 * @format
 */

import NativePerformanceObserver from './NativePerformanceObserver';
import {warnNoNativePerformanceObserver} from './PerformanceObserver';
import {rawToPerformanceEntryType} from './RawPerformanceEntry';

type EventCountsForEachCallbackType =
  | (() => void)
  | ((value: number) => void)
  | ((value: number, key: string) => void)
  | ((value: number, key: string, map: Map<string, number>) => void);

/**
 * Implementation of the EventCounts Web Performance API
 * corresponding to the standard in
 * https://www.w3.org/TR/event-timing/#eventcounts
 */
export interface EventCounts {
  size: number;

  +entries: () => Iterator<[string, number]>;
  +forEach: (callback: EventCountsForEachCallbackType) => void;
  +get: (key: string) => ?number;
  +has: (key: string) => boolean;
  +keys: () => Iterator<string>;
  +values: () => Iterator<number>;
}

let cachedEventCounts: ?Map<string, number>;

function getCachedEventCounts(): Map<string, number> {
  if (cachedEventCounts) {
    return cachedEventCounts;
  }
  if (!NativePerformanceObserver) {
    warnNoNativePerformanceObserver();
    return new Map();
  }

  cachedEventCounts = new Map<string, number>(
    NativePerformanceObserver.getEventCounts(),
  );
  // $FlowFixMe[incompatible-call]
  global.queueMicrotask(() => {
    // To be consistent with the calls to the API from the same task,
    // but also not to refetch the data from native too often,
    // schedule to invalidate the cache later,
    // after the current task is guaranteed to have finished.
    cachedEventCounts = null;
  });
  return cachedEventCounts ?? new Map();
}

// flowlint unsafe-getters-setters:off
export const EventCountsProxy: EventCounts = {
  get size(): number {
    return getCachedEventCounts().size;
  },

  set size(value: number) {
    throw new TypeError('EventCounts.size property is read-only');
  },

  entries: (): Iterator<[string, number]> => {
    return getCachedEventCounts().entries();
  },

  forEach: (callback: EventCountsForEachCallbackType) => {
    return getCachedEventCounts().forEach(callback);
  },

  get: (key: string): ?number => {
    return getCachedEventCounts().get(key);
  },

  has: (key: string): boolean => {
    return getCachedEventCounts().has(key);
  },

  keys: (): Iterator<string> => {
    return getCachedEventCounts().keys();
  },

  values: (): Iterator<number> => {
    return getCachedEventCounts().values();
  },
};
