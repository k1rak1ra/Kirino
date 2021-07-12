package net.k1ra.kirino;

import android.app.Dialog;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.RelativeDateTimeFormatter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.MenuView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class frag_list_manga extends Fragment {
    Media_DB DB;
    List<Media> media = new ArrayList<Media>();
    BroadcastReceiver receiver;
    BroadcastReceiver manga_chap_receiver = null;

    public static Fragment newInstance() {
        return new frag_list_manga();
    }

    @Override
    public void onDestroyView()
    {
        //dispose of DB connection and receiver
        DB.close();
        getActivity().unregisterReceiver(receiver);
        try {
            if (manga_chap_receiver != null) {
                getActivity().unregisterReceiver(manga_chap_receiver);
            }
        } catch (Exception e) {}

        super.onDestroyView();
    }

    @Override
    public void onResume()
    {
        getActivity().sendBroadcast(new Intent("net.k1ra.update.manga"));
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_recycler, container, false);

        //init DB
        DB = Room.databaseBuilder(getActivity(),
                Media_DB.class, "manga_db")
                .build();

        final RecyclerView list = rootView.findViewById(R.id.recycler);
        final TextView empty = rootView.findViewById(R.id.list_empty);
        final media_list_adapter adapter = new media_list_adapter();
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        list.setLayoutManager(llm);
        empty.setText(getText(R.string.manga_list_empty));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intnt) {
                Loading.show(getActivity());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        media = DB.DAO().fetch_all();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Loading.dismiss();
                                if (media.size() == 0) { empty.setVisibility(View.VISIBLE); } else {empty.setVisibility(View.INVISIBLE);}
                                list.setAdapter(adapter);
                            }
                        });
                    }
                }).start();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("net.k1ra.update.manga");
        getActivity().registerReceiver(receiver, filter);

        return rootView;
    }

    class media_list_adapter extends RecyclerView.Adapter<media_list_adapter.MediaViewHolder>{

        class MediaViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name;
            TextView air_rating;
            TextView new_ep_day_count;
            TextView type_num;
            CardView card;

            MediaViewHolder(View itemView) {
                super(itemView);
                    img = itemView.findViewById(R.id.a_l_img);
                    name = itemView.findViewById(R.id.a_l_name);
                    air_rating = itemView.findViewById(R.id.a_l_air_rating);
                    new_ep_day_count = itemView.findViewById(R.id.a_l_new_ep_day_count);
                    type_num = itemView.findViewById(R.id.a_l_type_num);
                    card = itemView.findViewById(R.id.a_card_simple);
            }
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getItemCount() {
            return media.size();
        }
        @Override
        public MediaViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.media_card, viewGroup, false);
            MediaViewHolder vh = new MediaViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final MediaViewHolder vh, final int i) {
            Tools.picasso.load(media.get(i).image_URL).into(vh.img);
            vh.name.setText(media.get(i).name);
            if (!media.get(i).score.equals("null")) {
                if (media.get(i).AirDate.equals(""))
                {
                    vh.air_rating.setText(media.get(i).score);
                }
                else {
                    vh.air_rating.setText(media.get(i).score + " - " + media.get(i).AirDate);
                }
            }
            else
            {
                vh.air_rating.setText(media.get(i).AirDate);
            }
            if (media.get(i).current) {
                vh.new_ep_day_count.setVisibility(View.VISIBLE);
                vh.new_ep_day_count.setText(getString(R.string.current_manga));
            } else { vh.new_ep_day_count.setVisibility(View.INVISIBLE); }

            if (!media.get(i).numEpisodes.equals("null")) {
                vh.type_num.setText(media.get(i).format.replace("_", " ").toLowerCase() + " - " + media.get(i).numEpisodes + " " + getString(R.string.chapters));
            }
            else
            {
                vh.type_num.setText(media.get(i).format.replace("_", " ").toLowerCase());
            }

            vh.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Dialog dialog = new Dialog(getActivity());
                    dialog.setCancelable(true);
                    dialog.setContentView(R.layout.media_card_dialog);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                    final Button manage = dialog.findViewById(R.id.m_c_d_manage);
                    final Button details = dialog.findViewById(R.id.m_c_d_details);
                    final Button pirate = dialog.findViewById(R.id.m_c_d_pirate);
                    final Button CR = dialog.findViewById(R.id.m_c_d_CR);

                    //hide streaming options since manga/LN desu
                    pirate.setVisibility(View.GONE);
                    CR.setVisibility(View.GONE);

                    //If it's a manga, we give the option to pirate from mangarock
                    if (media.get(i).format.replace("_", " ").toLowerCase().equals("manga"))
                    {
                        pirate.setVisibility(View.VISIBLE);
                        pirate.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                pirate(media.get(i));
                            }
                        });
                    }

                    manage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //not signed into AniList, just give option to delet. Since we're lazy we'll just reuse the download button dialog...
                            if (getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_token", "").equals("")) {
                                final Dialog manage = new Dialog(getActivity());
                                manage.setCancelable(true);
                                manage.setContentView(R.layout.dialog_feed_image_download);
                                manage.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                final Button delete = manage.findViewById(R.id.fa_delet_dis);
                                manage.findViewById(R.id.fa_download).setVisibility(View.GONE);
                                delete.setText(getString(R.string.delete));
                                delete.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.media_del)+" "+media.get(i).name+"?");

                                        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int j) {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        DB.DAO().delet_one(media.get(i).ALid);
                                                        getActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                media.remove(i);
                                                                notifyItemRemoved(i);
                                                                manage.dismiss();
                                                                dialog.dismiss();
                                                            }
                                                        });
                                                    }
                                                }).start();
                                            }
                                        });
                                        alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) { //do nothing, just dismiss alertdialog
                                            }
                                        });

                                        alert.show();
                                    }
                                });

                                manage.show();
                            } else { //signed into AniList, give all options
                                //the media object we'll be editing
                                final Media editing = media.get(i);

                                final Dialog manage = new Dialog(getActivity());
                                manage.setCancelable(true);
                                manage.setContentView(R.layout.media_card_dialog_manage);
                                manage.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                final EditText score = manage.findViewById(R.id.m_c_d_m_score);
                                final EditText progress = manage.findViewById(R.id.m_c_d_m_prog_chap);
                                final EditText progress_vol = manage.findViewById(R.id.m_c_d_m_prog_vol);
                                final EditText repeat = manage.findViewById(R.id.m_c_d_m_reread);
                                final TextView start = manage.findViewById(R.id.m_c_d_m_start);
                                final TextView end = manage.findViewById(R.id.m_c_d_m_end);
                                final Button start_change = manage.findViewById(R.id.m_c_d_m_start_change);
                                final Button end_change = manage.findViewById(R.id.m_c_d_m_end_change);
                                final Button save = manage.findViewById(R.id.m_c_d_m_save);
                                final Button drop = manage.findViewById(R.id.m_c_d_m_drop);
                                final Button delete = manage.findViewById(R.id.m_c_d_m_delete);
                                final Button complete = manage.findViewById(R.id.m_c_d_m_complete);

                                score.setText(String.valueOf(media.get(i).p_score));
                                progress.setText(String.valueOf(media.get(i).progress));
                                progress_vol.setText(String.valueOf(media.get(i).progress_volumes));
                                repeat.setText(String.valueOf(media.get(i).repeat));

                                if(media.get(i).started_year != 0)
                                {
                                    start.setText(getString(R.string.start_date)+" "+media.get(i).started_year+"/"+media.get(i).started_month+"/"+media.get(i).started_day);
                                }

                                if(media.get(i).ended_year != 0)
                                {
                                    end.setText(getString(R.string.end_date)+" "+media.get(i).ended_year+"/"+media.get(i).ended_month+"/"+media.get(i).ended_day);
                                }

                                start_change.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Tools.DatePicker(getActivity(), true, editing, start);
                                    }
                                });
                                end_change.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Tools.DatePicker(getActivity(), false, editing, end);
                                    }
                                });

                                save.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        try {
                                            editing.p_score = Float.parseFloat(score.getText().toString());
                                            editing.progress = Integer.parseInt(progress.getText().toString());
                                            editing.progress_volumes = Integer.parseInt(progress_vol.getText().toString());
                                            editing.repeat = Integer.parseInt(repeat.getText().toString());

                                            final NetworkResponse out = new NetworkResponse();
                                            NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, new Pair[]{Pair.create("query", "mutation {\n" +
                                                    "  SaveMediaListEntry(mediaId: " + editing.ALid + ", \n" +
                                                    "    score: "+editing.p_score+", \n" +
                                                    "    progress:"+editing.progress+", \n" +
                                                    "    progressVolumes:"+editing.progress_volumes+",\n" +
                                                    "    repeat:"+editing.repeat+",\n" +
                                                    "    startedAt:{\n" +
                                                    "      year:"+editing.started_year+",\n" +
                                                    "      month:"+editing.started_month+",\n" +
                                                    "      day:"+editing.started_day+"\n" +
                                                    "    },\n" +
                                                    "    completedAt:{\n" +
                                                    "      year:"+editing.ended_year+",\n" +
                                                    "      month:"+editing.ended_month+",\n" +
                                                    "      day:"+editing.ended_day+"\n" +
                                                    "    }) {\n" +
                                                    "    id\n" +
                                                    "  }\n" +
                                                    "}")}, getActivity(), new Runnable() {
                                                @Override
                                                public void run() {
                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            DB.DAO().update(editing);
                                                            media = DB.DAO().fetch_all();
                                                            getActivity().runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    manage.dismiss();
                                                                    dialog.dismiss();
                                                                }
                                                            });
                                                        }
                                                    }).start();
                                                }
                                            }, null);
                                        } catch (NumberFormatException e)
                                        {//nice try faget
                                            Errors.toast(getActivity(), getString(R.string.parse_error));
                                        }
                                    }
                                });

                                drop.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.media_drop)+" "+media.get(i).name+"?");

                                        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int j) {
                                                final NetworkResponse out = new NetworkResponse();
                                                NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, NetworkRequest.make_AL_query(6, "DROPPED", Integer.parseInt(media.get(i).ALid)), getActivity(), new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                DB.DAO().delet_one(media.get(i).ALid);
                                                                getActivity().runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        media.remove(i);
                                                                        notifyItemRemoved(i);
                                                                        manage.dismiss();
                                                                        dialog.dismiss();
                                                                    }
                                                                });
                                                            }
                                                        }).start();
                                                    }
                                                }, null);
                                            }
                                        });
                                        alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) { //do nothing, just dismiss alertdialog
                                            }
                                        });

                                        alert.show();
                                    }
                                });

                                complete.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.media_complete)+" "+media.get(i).name+"?");

                                        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int j) {
                                                final NetworkResponse out = new NetworkResponse();
                                                NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, NetworkRequest.make_AL_query(6, "COMPLETED", Integer.parseInt(media.get(i).ALid)), getActivity(), new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                DB.DAO().delet_one(media.get(i).ALid);
                                                                getActivity().runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        media.remove(i);
                                                                        notifyItemRemoved(i);
                                                                        manage.dismiss();
                                                                        dialog.dismiss();
                                                                    }
                                                                });
                                                            }
                                                        }).start();
                                                    }
                                                }, null);
                                            }
                                        });
                                        alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) { //do nothing, just dismiss alertdialog
                                            }
                                        });

                                        alert.show();
                                    }
                                });

                                delete.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.media_del)+" "+media.get(i).name+"?");

                                        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int j) {
                                                final NetworkResponse out = new NetworkResponse();
                                                NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, NetworkRequest.make_AL_query(8, media.get(i).ALid, 0, getActivity()), getActivity(), new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            final NetworkResponse out_2 = new NetworkResponse();
                                                            NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, NetworkRequest.make_AL_query(7, out.obj.getJSONObject("data").getJSONObject("Page").getJSONArray("mediaList").getJSONObject(0).getString("id"), 0), getActivity(), new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    new Thread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            DB.DAO().delet_one(media.get(i).ALid);
                                                                            getActivity().runOnUiThread(new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    media.remove(i);
                                                                                    notifyItemRemoved(i);
                                                                                    manage.dismiss();
                                                                                    dialog.dismiss();
                                                                                }
                                                                            });
                                                                        }
                                                                    }).start();
                                                                }
                                                            }, null);
                                                        } catch (JSONException e) {}
                                                    }
                                                }, null);
                                            }
                                        });
                                        alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) { //do nothing, just dismiss alertdialog
                                            }
                                        });

                                        alert.show();
                                    }
                                });

                                manage.show();
                            }
                        }
                    });

                    details.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //relay through anime list because feed fragment has been destroyed
                            Intent intent = new Intent("net.k1ra.relay");
                            intent.putExtra("type", 2);
                            intent.putExtra("id", media.get(i).ALid);
                            getActivity().sendBroadcast(intent);
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                }
            });

        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    void pirate(final Media manga)
    {
        manga.name = manga.name.replace("☆"," ");
        final NetworkResponse out = new NetworkResponse();
        NetworkRequest.make(out, NetworkRequest.MR_search, true, false, new Pair[]{Pair.create("", "{\"type\":\"series\",\"keywords\":\"" + manga.name + "\"}")}, getActivity(), new Runnable() {
            @Override
            public void run() {
                try {
                    final NetworkResponse meta_out = new NetworkResponse();
                    NetworkRequest.make(meta_out, NetworkRequest.MR_meta, true, false, new Pair[]{Pair.create("", out.obj.getJSONArray("data").toString())}, getActivity(), new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray results = out.obj.getJSONArray("data");

                                //use the same name-matching algorithm as used in AL_name_to_gelbooru_tag
                                int[] matching_score = new int[results.length()];

                                for (int j = 0; j < results.length(); j++) {
                                    try {
                                        if (meta_out.obj.getJSONObject("data").getJSONObject(results.getString(j)).getString("name").length() == manga.name.length()) {
                                            matching_score[j] = 1000000;
                                        }
                                        for (int q = 0; q < meta_out.obj.getJSONObject("data").getJSONObject(results.getString(j)).getString("name").length(); q++) {
                                            if (meta_out.obj.getJSONObject("data").getJSONObject(results.getString(j)).getString("name").toCharArray()[q] == manga.name.toCharArray()[q]) {
                                                matching_score[j]++;
                                            }
                                        }
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                    }
                                }
                                int max_index = 0;
                                for (int j = 0; j < matching_score.length; j++) {
                                    if (matching_score[max_index] < matching_score[j]) {
                                        max_index = j;
                                    }
                                }

                                final NetworkResponse chap_out = new NetworkResponse();
                                NetworkRequest.make(chap_out, NetworkRequest.MR_info, false, false, new Pair[]{Pair.create("oid", out.obj.getJSONArray("data").getString(max_index)), Pair.create("last", "0"), Pair.create("country", "Canada")}, getActivity(), new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final Dialog m_d = new Dialog(getActivity());
                                            m_d.setCancelable(true);
                                            m_d.setContentView(R.layout.episode_list_dialog);
                                            m_d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                            final RecyclerView m_list = m_d.findViewById(R.id.episode_list);
                                            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                                            m_list.setLayoutManager(llm);
                                            final chapter_list_adapter adapter = new chapter_list_adapter();
                                            adapter.chapters = chap_out.obj.getJSONObject("data").getJSONArray("chapters");
                                            getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit().putString(manga.ALid, chap_out.obj.getJSONObject("data").getJSONArray("chapters").toString()).commit();
                                            adapter.in = manga;
                                            m_list.setAdapter(adapter);
                                            register_reciever(manga, adapter);
                                            m_d.show();
                                        } catch (JSONException e) {
                                        }
                                    }
                                }, null);
                            } catch (JSONException e) {
                            }
                        }
                    }, null);
                } catch (JSONException e) {
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                try {
                    final Dialog m_d = new Dialog(getActivity());
                    m_d.setCancelable(true);
                    m_d.setContentView(R.layout.episode_list_dialog);
                    m_d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                    final RecyclerView m_list = m_d.findViewById(R.id.episode_list);
                    LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                    m_list.setLayoutManager(llm);
                    final chapter_list_adapter adapter = new chapter_list_adapter();
                    adapter.chapters = new JSONArray(getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(manga.ALid,""));
                    adapter.in = manga;
                    m_list.setAdapter(adapter);
                    register_reciever(manga, adapter);
                    m_d.show();
                } catch (JSONException e) {
                }
            }
        });
    }

    void register_reciever(final Media manga, final chapter_list_adapter adapter)
    {
        manga_chap_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intnt) {
                try {
                    if (!intnt.getBooleanExtra("skip_loop", false)) {
                        for (int i = 0; i < adapter.chapters.length(); i++) {
                            if (intnt.getIntExtra("page", -1) != -1 && intnt.getStringExtra("oid").equals(adapter.chapters.getJSONObject(i).getString("oid"))) {
                                getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit().putInt(intnt.getStringExtra("oid") + "-PROG", intnt.getIntExtra("page", -1)).commit();

                                int chapter_number = 0;
                                int volume_number = 0;
                                try {
                                    String[] parts = adapter.chapters.getJSONObject(i).getString("name").split("\\s+");

                                    if (parts[0].substring(0, 3).equals("Vol")) {
                                        volume_number = Integer.parseInt(parts[0].split("\\.")[1]);
                                        chapter_number = Integer.parseInt(parts[2].replaceAll("[^\\d.]", "").split("\\.")[0]);
                                    } else {
                                        chapter_number = Integer.parseInt(parts[1].replaceAll("[^\\d.]", "").split("\\.")[0]);
                                    }
                                } catch (Exception e) {
                                }

                                if ((int) (((((float) getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getInt(adapter.chapters.getJSONObject(i).getString("oid") + "-PROG", -1))) /
                                        (float) new JSONArray(getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(adapter.chapters.getJSONObject(i).getString("oid"), "null")).length()) * 100) >= 100 && chapter_number > manga.progress) {
                                    Calendar cal = new GregorianCalendar();

                                    manga.progress = chapter_number;
                                    manga.progress_volumes = volume_number;

                                    if (manga.progress == 1) {
                                        manga.started_year = cal.get(Calendar.YEAR);
                                        manga.started_month = cal.get(Calendar.MONTH) + 1;
                                        manga.started_day = cal.get(Calendar.DAY_OF_MONTH);
                                    }

                                    try {
                                        //if this was the last chapter, set end day as today
                                        if (manga.progress == Integer.parseInt(manga.numEpisodes)) {
                                            manga.ended_year = cal.get(Calendar.YEAR);
                                            manga.ended_month = cal.get(Calendar.MONTH) + 1;
                                            manga.ended_day = cal.get(Calendar.DAY_OF_MONTH);
                                        }
                                    } catch (Exception e) {
                                    }

                                    if (getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getBoolean("sync_prog_m", true)) {
                                        final NetworkResponse out = new NetworkResponse();
                                        NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, new Pair[]{Pair.create("query", "mutation {\n" +
                                                "  SaveMediaListEntry(mediaId: " + manga.ALid + ", \n" +
                                                "    score: " + manga.p_score + ", \n" +
                                                "    progress:" + manga.progress + ", \n" +
                                                "    repeat:" + manga.repeat + ",\n" +
                                                "    startedAt:{\n" +
                                                "      year:" + manga.started_year + ",\n" +
                                                "      month:" + manga.started_month + ",\n" +
                                                "      day:" + manga.started_day + "\n" +
                                                "    },\n" +
                                                "    completedAt:{\n" +
                                                "      year:" + manga.ended_year + ",\n" +
                                                "      month:" + manga.ended_month + ",\n" +
                                                "      day:" + manga.ended_day + "\n" +
                                                "    }) {\n" +
                                                "    id\n" +
                                                "  }\n" +
                                                "}")}, getActivity(), new Runnable() {
                                            @Override
                                            public void run() {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        DB.DAO().update(manga);
                                                        media = DB.DAO().fetch_all();
                                                    }
                                                }).start();
                                            }
                                        }, null);
                                    }

                                }
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    System.out.println("DATASET CHANGED!!");
                } catch (JSONException e) { e.printStackTrace(); }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("net.k1ra.update.manga.chap");
        getActivity().registerReceiver(manga_chap_receiver, filter);
    }

    class chapter_list_adapter extends RecyclerView.Adapter<chapter_list_adapter.ChapterViewHolder>{

        JSONArray chapters;
        Media in;

        class ChapterViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ProgressBar bar;
            ProgressBar downloading;
            ImageView downloaded;
            int chapter_number = 0;
            int volume_number = 0;

            ChapterViewHolder(View itemView) {
                super(itemView);
                name = (TextView)itemView.findViewById(R.id.chapter_name);
                bar = itemView.findViewById(R.id.chapter_bar);
                downloaded = itemView.findViewById(R.id.chapter_downloaded);
                downloading = itemView.findViewById(R.id.chapter_downloading);
            }
        }
        @Override
        public int getItemCount() {
            return chapters.length();
        }
        @Override
        public ChapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chapter_list_card, viewGroup, false);
            ChapterViewHolder vh = new ChapterViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final ChapterViewHolder vh, final int i) {
            try {
                try {
                    String[] parts = chapters.getJSONObject(i).getString("name").split("\\s+");

                    if (parts[0].substring(0, 3).equals("Vol")) {
                        vh.volume_number = Integer.parseInt(parts[0].split("\\.")[1]);
                        vh.chapter_number = Integer.parseInt(parts[2].replaceAll("[^\\d.]", "").split("\\.")[0]);
                    } else {
                        vh.chapter_number = Integer.parseInt(parts[1].replaceAll("[^\\d.]", "").split("\\.")[0]);
                    }
                } catch (Exception e) {}

                vh.name.setText(chapters.getJSONObject(i).getString("name"));

                //use precise page data, if available
                if (getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getInt(chapters.getJSONObject(i).getString("oid")+"-PROG", -1) != -1 && !getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(chapters.getJSONObject(i).getString("oid"), "null").equals("null"))
                {
                    vh.bar.setProgress((int) (((((float) getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getInt(chapters.getJSONObject(i).getString("oid")+"-PROG", -1))) / (float) new JSONArray(getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(chapters.getJSONObject(i).getString("oid"), "null")).length()) * 100));
                } else if (vh.chapter_number <= in.progress) {
                    vh.bar.setProgress(100);
                } else {
                    vh.bar.setProgress(0);
                }

                vh.downloading.setVisibility(View.GONE);
                if (getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(chapters.getJSONObject(i).getString("oid"), "null").equals("null")) {
                    vh.downloaded.setVisibility(View.INVISIBLE);
                    vh.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                           prepare_download(vh.downloading, chapters, i, getActivity());
                        }
                    });
                } else {
                    vh.downloaded.setVisibility(View.VISIBLE);
                    vh.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                                //download next chapter in the background so it is ready
                                prepare_download(null, chapters, i+1, getActivity());

                                //and open manga reader on selected chapter
                                Intent intent = new Intent(getActivity(), fullscreen_manga_reader.class);
                                intent.putExtra("chapters", chapters.toString());
                                intent.putExtra("index", i);
                                startActivity(intent);
                        }
                    });
                    vh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            if (vh.downloaded.getVisibility() == View.VISIBLE) {
                                final Dialog manage = new Dialog(getActivity());
                                manage.setCancelable(true);
                                manage.setContentView(R.layout.dialog_feed_image_download);
                                manage.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                final Button delete = manage.findViewById(R.id.fa_delet_dis);
                                manage.findViewById(R.id.fa_download).setVisibility(View.GONE);
                                delete.setText(getString(R.string.delete));
                                delete.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        try {
                                            final JSONArray pages = new JSONArray(getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(chapters.getJSONObject(i).getString("oid"), "null"));

                                            for (int i = 0; i < pages.length(); i++) {
                                                try {
                                                    File imgFile = new File(getActivity().getExternalFilesDir(null), pages.getString(i).split("\\/")[pages.getString(i).split("\\/").length - 1]);
                                                    if (imgFile.exists()) {
                                                        imgFile.delete();
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit().putString(chapters.getJSONObject(i).getString("oid"), "null").commit();
                                            manage.dismiss();
                                            final Intent intent = new Intent("net.k1ra.update.manga.chap");
                                            intent.putExtra("skip_loop", true);
                                            getActivity().sendBroadcast(intent);
                                        } catch (JSONException e) {
                                        }
                                    }
                                });

                                manage.show();
                                return true;
                            } else { return false; }
                        }
                    });
                }

            } catch (JSONException e) {}
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }

    static void prepare_download(final ProgressBar downloading, final JSONArray chapters, final int i, final Context c)
    {
        try {
            final NetworkResponse out = new NetworkResponse();
            NetworkRequest.make(out, NetworkRequest.MR_chapter, false, false, new Pair[]{Pair.create("oid", chapters.getJSONObject(i).getString("oid")), Pair.create("country", "Canada")}, c, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (downloading != null) {downloading.setVisibility(View.VISIBLE);}
                        store_images(out.obj.getJSONArray("data"), chapters.getJSONObject(i).getString("oid"), c);
                    } catch (JSONException e) {
                    }
                }
            }, null);
        } catch (JSONException e) {}
    }

    static float completed_count = 0;
    static void store_images(final JSONArray data, final String oid, final Context c)
    {
        completed_count = 0;
        for (int i = 0; i < data.length(); i++)
        {
            try {
                final NetworkResponse out = new NetworkResponse();
                out.supp = i;
                NetworkRequest.make(out, new NetworkRequest.Parameters(data.getString(out.supp)).noStrict().is_MR_img(), false, false, null, c, new Runnable() {
                    @Override
                    public void run() {
                        FileOutputStream file_write = null;
                        try {
                            File file = new File(c.getExternalFilesDir(null), data.getString(out.supp).split("\\/")[data.getString(out.supp).split("\\/").length - 1]);
                            file_write = new FileOutputStream(file);
                            file_write.write(out.bytes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                file_write.flush();
                                file_write.close();
                                completed_count++;

                                        if (((completed_count / (float) data.length()) * 100) >= 100) {
                                            c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit().putString(oid, data.toString()).commit();
                                            final Intent intent = new Intent("net.k1ra.update.manga.chap");
                                            intent.putExtra("skip_loop", true);
                                            c.sendBroadcast(intent);
                                        }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        Errors.no_internet_toast(c);
                    }
                });
            } catch (Exception e) {}
        }
    }
}
