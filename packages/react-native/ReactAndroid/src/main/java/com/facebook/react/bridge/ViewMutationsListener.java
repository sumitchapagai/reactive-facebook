/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.bridge;

import com.facebook.react.fabric.mounting.mountitems.IntBufferMountItem;

import java.util.ArrayList;

public interface ViewMutationsListener {
    /**
     * Called right before view mutations are dispatched. This is useful if a
     * module needs to do something before the views are created/removed.
     */
    void willMountViewMutations(ArrayList<IntBufferMountItem> mutations);
}
