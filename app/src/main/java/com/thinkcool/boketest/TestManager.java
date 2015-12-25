package com.thinkcool.boketest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thinkcool on 2015/12/24.
 */
public class TestManager {
    public static final TestManager INSTANCE = new TestManager();
    private List<MyListener> mListenerList;

    private TestManager() {
        mListenerList = new ArrayList<MyListener>();
    }

    public static TestManager getInstance() {
        return INSTANCE;
    }

    public void registerListener(MyListener listener) {
        if (!mListenerList.contains(listener)) {
            mListenerList.add(listener);
        }
    }
    public void unregisterListener(MyListener listener) {
        mListenerList.remove(listener);
    }
}

interface MyListener {
    public void onSomeThingHappen();
}
