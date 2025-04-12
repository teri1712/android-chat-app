package com.decade.practice;

import org.junit.Rule;
import org.junit.Test;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class HiltTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);
    
    @Test
    public void runTest() {
    }
}
