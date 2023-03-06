package br.com.jvsdermatologia.walker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ListTracks extends AppCompatActivity {

    private ListView listView;
    private List<Track> tracks = new ArrayList<>();
    private Context context;
    private static Track sentTrack;
    private MyAdapter myAdapter;

    public static Track getSentTrack() {
        return sentTrack;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_tracks);

        listView = findViewById(R.id.list);
        context = this;

        List<File> files = Utils.getUtils().listFiles(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS));
        files.sort(new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                Long f1 = Long.valueOf(file.getName().replace(".trk", ""));
                Long f2 = Long.valueOf(file2.getName().replace(".trk", ""));
                return (int) (f2 - f1);
            }
        });
        for (File file : files) {
            String name = file.getName().replace(".trk", "");
            String content = Utils.getUtils().readTrack(this, name);
            Track track1 = new Gson().fromJson(content, Track.class);
            tracks.add(track1);
        }

        myAdapter = new MyAdapter(tracks, this);

        listView.setAdapter(myAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    synchronized public void onClickApagar(MenuItem item) {
        Set<String> setDelete = myAdapter.getSetToDelete();
        if (setDelete.isEmpty())
            Toast.makeText(context, getString(R.string.noItem), Toast.LENGTH_SHORT).show();
        else {
            confirmDelete(setDelete);
        }
    }

    public void confirmDelete(final Set<String> setDelete) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.delete) + " " + setDelete.size() + " " + getString(R.string.tracks) + "?")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for (final String trackFile : setDelete) {
                            if (Utils.getUtils().deleteFile(context, trackFile)) {
                                tracks.removeIf(new Predicate<Track>() {
                                    @Override
                                    public boolean test(Track track) {
                                        return track.getEnd() == Long.parseLong(trackFile);
                                    }
                                });
                            }
                        }
                        myAdapter.updateTracks(tracks);
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    public class MyAdapter extends BaseAdapter {

        private Track[] tracks;
        private Context context;
        private TextView textViewTitle;
        private TextView textViewSub1;
        private TextView textViewSub2;
        private LinearLayout linearLayout;
        private LayoutInflater layoutInflater;
        private CheckBox checkBox;
        private final Set<String> setToDelete = new HashSet<>();

        public MyAdapter(List<Track> tracks, Context context) {
            Track[] trackArr = new Track[tracks.size()];
            for (int i = 0; i < trackArr.length; i++) {
                trackArr[i] = tracks.get(i);
            }
            this.tracks = trackArr;
            this.context = context;
        }

        public Set<String> getSetToDelete() {
            return setToDelete;
        }

        public void updateTracks(List<Track> tracks) {
            Track[] trackArr = new Track[tracks.size()];
            for (int i = 0; i < trackArr.length; i++) {
                trackArr[i] = tracks.get(i);
            }
            this.tracks = trackArr;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return tracks.length;
        }

        @Override
        public Object getItem(int i) {
            return tracks[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (layoutInflater == null)
                layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (view == null) view = layoutInflater.inflate(R.layout.list_item, viewGroup, false);

            Long start = tracks[i].getEnd() - (int) tracks[i].getTime() * 1000;
            Date date = new Date(start);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String dateString = simpleDateFormat.format(date);

            checkBox = view.findViewById(R.id.checkBox);
            checkBox.setChecked(false);
            checkBox.setTag(Long.toString(tracks[i].getEnd()));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    final String number = (String) compoundButton.getTag();
                    if (b) setToDelete.add(number);
                    else setToDelete.removeIf(new Predicate<String>() {
                        @Override
                        public boolean test(String s) {
                            return s.equals(number);
                        }
                    });
                }
            });

            textViewTitle = view.findViewById(R.id.textView11);
            textViewTitle.setText(dateString);

            linearLayout = view.findViewById(R.id.trackDescription);
            linearLayout.setTag(i);
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sentTrack = tracks[(int) view.getTag()];
                    Intent intent = new Intent(context, MapsActivity.class);
                    intent.putExtra("fromList", true);
                    startActivity(intent);
                }
            });

            textViewSub1 = view.findViewById(R.id.textView12);
            textViewSub1.setText(String.format("%.2f m", tracks[i].getDistance()));

            double tempo = tracks[i].getTime() / 60.0;
            int hours = (int) Math.floor(tempo / 60);
            int minutes = (int) Math.floor(tempo % 60);

            textViewSub2 = view.findViewById(R.id.textView13);
            textViewSub2.setText(String.format("%d h %d m", hours, minutes));

            return view;
        }
    }
}