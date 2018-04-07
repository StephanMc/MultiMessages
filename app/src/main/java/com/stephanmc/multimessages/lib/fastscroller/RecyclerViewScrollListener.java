//@formatter:off
/*
    Copyright 2015 Future Mind
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package com.stephanmc.multimessages.lib.fastscroller;


import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michal on 04/08/16.
 * Responsible for updating the handle / bubble position when user scrolls the {@link
 * android.support.v7.widget.RecyclerView}.
 */
public class RecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private final FastScroller scroller;
    List<RecyclerViewScrollListener.ScrollerListener> listeners = new ArrayList<>();
    int oldScrollState = RecyclerView.SCROLL_STATE_IDLE;

    public RecyclerViewScrollListener(FastScroller scroller) {
        this.scroller = scroller;
    }

    public void addScrollerListener(RecyclerViewScrollListener.ScrollerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newScrollState) {
        super.onScrollStateChanged(recyclerView, newScrollState);
        if (newScrollState == RecyclerView.SCROLL_STATE_IDLE && oldScrollState != RecyclerView.SCROLL_STATE_IDLE) {
            scroller.getViewProvider().onScrollFinished();
        } else if (newScrollState != RecyclerView.SCROLL_STATE_IDLE
                && oldScrollState == RecyclerView.SCROLL_STATE_IDLE) {
            scroller.getViewProvider().onScrollStarted();
        }
        oldScrollState = newScrollState;
    }

    @Override
    public void onScrolled(RecyclerView rv, int dx, int dy) {
        if (scroller.shouldUpdateHandlePosition()) {
            updateHandlePosition(rv);
        }
    }

    void updateHandlePosition(RecyclerView rv) {
        float relativePos;
        if (scroller.isVertical()) {
            int offset = rv.computeVerticalScrollOffset();
            int extent = rv.computeVerticalScrollExtent();
            int range = rv.computeVerticalScrollRange();
            relativePos = offset / (float) (range - extent);
        } else {
            int offset = rv.computeHorizontalScrollOffset();
            int extent = rv.computeHorizontalScrollExtent();
            int range = rv.computeHorizontalScrollRange();
            relativePos = offset / (float) (range - extent);
        }
        scroller.setScrollerPosition(relativePos);
        notifyListeners(relativePos);
    }

    public void notifyListeners(float relativePos) {
        for (RecyclerViewScrollListener.ScrollerListener listener : listeners) {
            listener.onScroll(relativePos);
        }
    }

    public interface ScrollerListener {
        void onScroll(float relativePos);
    }

}
