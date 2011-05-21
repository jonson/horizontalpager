package com.github.ysamlan.horizontalpager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainPagerDemo extends Activity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		((Button)findViewById(R.id.horizontal_btn)).setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(MainPagerDemo.this, 
							TabbedHorizontalPagerDemo.class));
				}
			}
		);
		
		((Button)findViewById(R.id.vertical_btn)).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainPagerDemo.this, 
								TabbedVerticalPagerDemo.class));
					}
				}
			);
	}
}
