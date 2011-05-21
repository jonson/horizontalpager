package com.github.ysamlan.horizontalpager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * A more complex demo including using a RadioGroup as "tabs" for the pager and showing the
 * dual-scrolling capabilities when a vertically scrollable element is nested inside the pager.
 */
public class TabbedVerticalPagerDemo extends BasePagerDemoActivity {
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tabbed_vertical_pager_demo);
		init();
	}
	
	@Override
	protected void onSimplePagerButtonClick(View v) {
		startActivity(new Intent(TabbedVerticalPagerDemo.this,
                VerticalPagerDemo.class));
	}
    
}
