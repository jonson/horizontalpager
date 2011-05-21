package com.github.ysamlan.horizontalpager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

public abstract class BasePagerDemoActivity extends Activity {

	protected AbstractPager mPager;
    protected RadioGroup mRadioGroup;
	
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    protected void init() {
    	mPager = (AbstractPager) findViewById(R.id.horizontal_pager);
        mPager.setOnScreenSwitchListener(onScreenSwitchListener);
        mRadioGroup = (RadioGroup) findViewById(R.id.tabs);
        mRadioGroup.setOnCheckedChangeListener(onCheckedChangedListener);

        // Go to the simple demo if the user clicks on the simple demo button
        findViewById(R.id.simple_demo_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onSimplePagerButtonClick(v);
            }
        });
    }
    
    protected abstract void onSimplePagerButtonClick(View v);
    
	protected final AbstractPager.OnScreenSwitchListener onScreenSwitchListener =
        new HorizontalPager.OnScreenSwitchListener() {
            @Override
            public void onScreenSwitched(final int screen) {
                // Check the appropriate button when the user swipes screens.
                switch (screen) {
                    case 0:
                        mRadioGroup.check(R.id.radio_btn_0);
                        break;
                    case 1:
                        mRadioGroup.check(R.id.radio_btn_1);
                        break;
                    case 2:
                        mRadioGroup.check(R.id.radio_btn_2);
                        break;
                    default:
                        break;
                }
            }
        };

   protected final RadioGroup.OnCheckedChangeListener onCheckedChangedListener =
        new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final RadioGroup group, final int checkedId) {
                // Slide to the appropriate screen when the user checks a button.
                switch (checkedId) {
                    case R.id.radio_btn_0:
                        mPager.setCurrentScreen(0, true);
                        break;
                    case R.id.radio_btn_1:
                        mPager.setCurrentScreen(1, true);
                        break;
                    case R.id.radio_btn_2:
                        mPager.setCurrentScreen(2, true);
                        break;
                    default:
                        break;
                }
            }
        };
	
}
