package com.app.contactpicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ContactPicker extends Activity
	{

		@Override
		public void onCreate(Bundle icicle)
			{
				super.onCreate(icicle);
				setContentView(R.layout.contact_picker_main);

				Intent intent = getIntent();
				String dataPath = intent.getData().toString();

				final Uri data = Uri.parse(dataPath + "people/");
				final Cursor c = managedQuery(data, null, null, null, null);
				String[] from = new String[]
					{ People.NAME };
				int[] to = new int[]
					{ R.id.itemTextView };

				SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.listitemlayout, c, from, to);

				ListView lv = (ListView) findViewById(R.id.contactListView);
				lv.setAdapter(adapter);
				lv.setOnItemClickListener(new OnItemClickListener()
					{
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
							{
								// Переместите Курсор к выбранному элементу.
								c.moveToPosition(pos);
								// Извлеките идентификатор строки.
								int rowId = c.getInt(c.getColumnIndexOrThrow("_id"));
								// Соберите воедино результирующий путь URI.
								Uri outURI = Uri.parse(data.toString() + rowId);
								Intent outData = new Intent();
								outData.setData(outURI);
								setResult(Activity.RESULT_OK, outData);
								finish();
							}
					});

			}

	}
