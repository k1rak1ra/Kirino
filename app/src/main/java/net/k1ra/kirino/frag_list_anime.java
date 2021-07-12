package net.k1ra.kirino;

import android.app.Dialog;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.zip.CRC32;

import static android.view.View.GONE;

public class frag_list_anime extends Fragment {
    Media_DB DB;
    List<Media> media = new ArrayList<Media>();
    BroadcastReceiver receiver;
    BroadcastReceiver relay;
    BroadcastReceiver playhead_listener = null;

    public static Fragment newInstance() {
        return new frag_list_anime();
    }

    @Override
    public void onDestroyView()
    {
        //dispose of DB connection and receivers
        DB.close();
        getActivity().unregisterReceiver(receiver);
        getActivity().unregisterReceiver(relay);
        if (playhead_listener != null) {
            getActivity().unregisterReceiver(playhead_listener);
            playhead_listener = null;
        }

        super.onDestroyView();
    }

    @Override
    public void onResume()
    {
        getActivity().sendBroadcast(new Intent("net.k1ra.update.anime"));
        super.onResume();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_recycler, container, false);

        //init DB
        DB = Room.databaseBuilder(getActivity(),
                Media_DB.class, "anime_db").fallbackToDestructiveMigration()
                .build();

        final RecyclerView list = rootView.findViewById(R.id.recycler);
        final TextView empty = rootView.findViewById(R.id.list_empty);
        final media_list_adapter adapter = new media_list_adapter();
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        list.setLayoutManager(llm);
        empty.setText(getText(R.string.anime_list_empty));

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
        filter.addAction("net.k1ra.update.anime");
        getActivity().registerReceiver(receiver, filter);

        getActivity().sendBroadcast(new Intent("net.k1ra.update.anime"));

        relay= new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intnt) {
                Intent intent = new Intent("net.k1ra.update.feed_add");
                intent.putExtra("type", intnt.getIntExtra("type", 0));
                intent.putExtra("id", intnt.getStringExtra("id"));
                getActivity().sendBroadcast(intent);
            }
        };
        IntentFilter relay_filter = new IntentFilter();
        relay_filter.addAction("net.k1ra.relay");
        getActivity().registerReceiver(relay, relay_filter);

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
                long diff_millis = (media.get(i).new_ep_day*1000) - new Date().getTime();
                int days = (int)(diff_millis/86400000);
                diff_millis -= days*86400000;

                Calendar today_begin = new GregorianCalendar();
                today_begin.set(Calendar.HOUR_OF_DAY, 0);
                today_begin.set(Calendar.MINUTE, 0);
                today_begin.set(Calendar.SECOND, 0);
                today_begin.set(Calendar.MILLISECOND, 0);

                if  ((new Date().getTime() - today_begin.getTimeInMillis())+diff_millis > 86400000)
                {
                    days++;
                }

                if (days < 0) { vh.new_ep_day_count.setVisibility(View.INVISIBLE); }
                else if (days == 0) { vh.new_ep_day_count.setText(getString(R.string.ep_today)); }
                else if (days == 1) { vh.new_ep_day_count.setText(getString(R.string.ep_tomorrow)); }
                else
                {
                    vh.new_ep_day_count.setText(getString(R.string.new_episode_in)+" "+days+" "+getString(R.string.days));
                }

            }
            else if (!media.get(i).has_started_airing)
            {
                vh.new_ep_day_count.setText(getString(R.string.not_yet_aired));
            }
            else { vh.new_ep_day_count.setVisibility(View.INVISIBLE); }
            if (!media.get(i).numEpisodes.equals("null")) {
                vh.type_num.setText(media.get(i).format + " - " + media.get(i).numEpisodes + " " + getString(R.string.episodes));
            }
            else
            {
                vh.type_num.setText(media.get(i).format);
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

                    if (!media.get(i).has_started_airing)
                    {
                        pirate.setVisibility(GONE);
                        CR.setVisibility(GONE);
                    }

                    pirate.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Pirate(media.get(i));
                        }
                    });

                    //only show CR button if signed into CR and anime is on CR
                    if (media.get(i).streamType == 1 && getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getBoolean("CR_logged_in", false)) {
                        CR.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CR_init_and_show_episodes(media.get(i));
                            }
                        });
                    }
                    else { CR.setVisibility(GONE); }

                    details.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent("net.k1ra.update.feed_add");
                            intent.putExtra("type", 2);
                            intent.putExtra("id", media.get(i).ALid);
                            getActivity().sendBroadcast(intent);
                            dialog.dismiss();
                        }
                    });

                    manage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //not signed into AniList, just give option to delet. Since we're lazy we'll just reuse the download button dialog...
                            if (getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_token", "").equals("")) {
                                final Dialog manage = new Dialog(getActivity());
                                manage.setCancelable(true);
                                manage.setContentView(R.layout.dialog_feed_image_download);
                                manage.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                final Button download = manage.findViewById(R.id.fa_download);
                                final Button delete = manage.findViewById(R.id.fa_delet_dis);
                                download.setVisibility(GONE);
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
                                manage.setContentView(R.layout.media_card_dialog_manage_a);
                                manage.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                final EditText score = manage.findViewById(R.id.m_c_d_m_a_score);
                                final EditText progress = manage.findViewById(R.id.m_c_d_m_a_prog);
                                final EditText repeat = manage.findViewById(R.id.m_c_d_m_a_rewatch);
                                final TextView start = manage.findViewById(R.id.m_c_d_m_a_start);
                                final TextView end = manage.findViewById(R.id.m_c_d_m_a_end);
                                final Button start_change = manage.findViewById(R.id.m_c_d_m_a_start_change);
                                final Button end_change = manage.findViewById(R.id.m_c_d_m_a_end_change);
                                final Button save = manage.findViewById(R.id.m_c_d_m_a_save);
                                final Button drop = manage.findViewById(R.id.m_c_d_m_a_drop);
                                final Button delete = manage.findViewById(R.id.m_c_d_m_a_delete);
                                final Button complete = manage.findViewById(R.id.m_c_d_m_a_complete);

                                score.setText(String.valueOf(media.get(i).p_score));
                                progress.setText(String.valueOf(media.get(i).progress));
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
                                            editing.repeat = Integer.parseInt(repeat.getText().toString());

                                            final NetworkResponse out = new NetworkResponse();
                                            NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, new Pair[]{Pair.create("query", "mutation {\n" +
                                                    "  SaveMediaListEntry(mediaId: " + editing.ALid + ", \n" +
                                                    "    score: "+editing.p_score+", \n" +
                                                    "    progress:"+editing.progress+", \n" +
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



                    dialog.show();
                }
            });

        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    int seconds;
    void CR_init_and_show_episodes(final Media in)
    {
        final NetworkResponse out = new NetworkResponse();
        NetworkRequest.make(out, NetworkRequest.AniList, true, false, NetworkRequest.make_AL_query(9, in.ALid, 0), getActivity(), new Runnable() {
            @Override
            public void run() {
                try {
                    //try and get episode length data from anilist. Otherwise, fallback to default 24 minutes
                    try { seconds = out.obj.getJSONObject("data").getJSONObject("Media").getInt("duration")*60; }
                    catch (JSONException e) { seconds = 24*60; }

                    final JSONArray episodes = out.obj.getJSONObject("data").getJSONObject("Media").getJSONArray("streamingEpisodes");

                    //Batch playhead value requests to crunchyroll
                    final int[] playheads = new int[episodes.length()];
                    final int[] episode_numbers = new int[episodes.length()];
                    int episode_offset = 0;

                    //some episode lists are in ascending order, and some are in descending order. Ascertain the order and put episode numbers into array
                    for (int i = 0; i < episode_numbers.length; i++)
                    { try {
                        episode_numbers[i] = Integer.parseInt(episodes.getJSONObject(i).getString("title").replace("-","").replace(".","-").replaceAll("[^-?0-9]+", " ").split("\\s+")[1]);
                    }catch (NumberFormatException |ArrayIndexOutOfBoundsException e) { episode_numbers[i] = -1; }}

                    //set offset in case episode numbers don't start at 1
                    if (episode_numbers.length != 0) {
                        if (episode_numbers[0] == 0 || episode_numbers[episode_numbers.length - 1] == 0) {
                            episode_offset = -1;
                        } else if (episode_numbers[0] > 1 && episode_numbers[episode_numbers.length - 1] > 1) {
                            if (episode_numbers[0] > episode_numbers[episode_numbers.length - 1]) {
                                episode_offset = episode_numbers[episode_numbers.length - 1] - 1;
                            } else {
                                episode_offset = episode_numbers[0] - 1;
                            }
                        }
                    }
                    final int episode_offset_f = episode_offset; //screw you, java

                    String requests = "[{\"method_version\":\"0\",\"api_method\":\"info\",\"params\":{\"media_id\":\""+episodes.getJSONObject(0).getString("url").split("\\-")[episodes.getJSONObject(0).getString("url").split("\\-").length - 1]+"\",\"fields\":\"media.playhead\"}}";
                    for (int i = 1; i < episodes.length(); i++) {
                        requests +=",{\"method_version\":\"0\",\"api_method\":\"info\",\"params\":{\"media_id\":\""+episodes.getJSONObject(i).getString("url").split("\\-")[episodes.getJSONObject(i).getString("url").split("\\-").length - 1]+"\",\"fields\":\"media.playhead\"}}"; }
                    requests += "]";
                    final NetworkResponse playheads_out = new NetworkResponse();
                    NetworkRequest.make(playheads_out, NetworkRequest.CR_batch, true, false, new Pair[]{Pair.create("requests", requests)}, getActivity(), new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray data = playheads_out.obj.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    playheads[i] = data.getJSONObject(i).getJSONObject("body").getJSONObject("data").getInt("playhead"); }

                                Dialog episodes_diag = new Dialog(getActivity());
                                episodes_diag.setContentView(R.layout.episode_list_dialog);
                                episodes_diag.setCancelable(true);
                                episodes_diag.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                RecyclerView ep_list = episodes_diag.findViewById(R.id.episode_list);
                                LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                                ep_list.setLayoutManager(llm);

                                final episode_list_adapter adapter = new episode_list_adapter();
                                adapter.episodes = episodes;
                                adapter.playheads = new int[playheads.length];
                                System.arraycopy(playheads, 0, adapter.playheads, 0, playheads.length);
                                adapter.seconds = seconds;
                                ep_list.setAdapter(adapter);

                                episodes_diag.show();

                                playhead_listener = new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        try {
                                            final int i = intent.getIntExtra("index", 0);
                                            //update playhead in list
                                            adapter.playheads[i] = (int) intent.getLongExtra("playhead", 0);
                                            adapter.notifyItemChanged(i);

                                            //update on crunchyroll
                                            final NetworkResponse CR_out = new NetworkResponse();
                                            NetworkRequest.make(CR_out, NetworkRequest.CR_log, true, false, new Pair[]{Pair.create("event", "playback_status"),
                                                    Pair.create("media_id", adapter.episodes.getJSONObject(i).getString("url").split("\\-")[adapter.episodes.getJSONObject(i).getString("url").split("\\-").length - 1]),
                                            Pair.create("playhead",String.valueOf(intent.getLongExtra("playhead",0)))}, getActivity(), null, null);

                                            //if over 85% of episode was watched, it will be counted as completed
                                            if (((float)intent.getLongExtra("playhead", 0)) / (float)seconds > 0.85 && episode_numbers[i] != -1 && episode_numbers[i] - episode_offset_f > in.progress && getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getBoolean("sync_prog_a", true)) {
                                                Calendar cal = new GregorianCalendar();
                                                in.progress = episode_numbers[i] - episode_offset_f;

                                                //if this was the first or 0th episode, set start date as today
                                                if (in.progress == 1) {
                                                    in.started_year = cal.get(Calendar.YEAR);
                                                    in.started_month = cal.get(Calendar.MONTH)+1;
                                                    in.started_day = cal.get(Calendar.DAY_OF_MONTH);
                                                }
                                                //if this was the last episode, set end day as today
                                                if (in.progress == Integer.parseInt(in.numEpisodes)) {
                                                    in.ended_year = cal.get(Calendar.YEAR);
                                                    in.ended_month = cal.get(Calendar.MONTH)+1;
                                                    in.ended_day = cal.get(Calendar.DAY_OF_MONTH);
                                                }

                                                final NetworkResponse out = new NetworkResponse();
                                                NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, new Pair[]{Pair.create("query", "mutation {\n" +
                                                        "  SaveMediaListEntry(mediaId: " + in.ALid + ", \n" +
                                                        "    score: " + in.p_score + ", \n" +
                                                        "    progress:" + in.progress + ", \n" +
                                                        "    repeat:" + in.repeat + ",\n" +
                                                        "    startedAt:{\n" +
                                                        "      year:" + in.started_year + ",\n" +
                                                        "      month:" + in.started_month + ",\n" +
                                                        "      day:" + in.started_day + "\n" +
                                                        "    },\n" +
                                                        "    completedAt:{\n" +
                                                        "      year:" + in.ended_year + ",\n" +
                                                        "      month:" + in.ended_month + ",\n" +
                                                        "      day:" + in.ended_day + "\n" +
                                                        "    }) {\n" +
                                                        "    id\n" +
                                                        "  }\n" +
                                                        "}")}, getActivity(), new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                DB.DAO().update(in);
                                                                media = DB.DAO().fetch_all();
                                                            }
                                                        }).start();
                                                    }
                                                }, null);
                                            }
                                        } catch (JSONException e) { e.printStackTrace(); }
                                    }
                                };
                                getActivity().registerReceiver(playhead_listener,new IntentFilter("net.k1ra.CR_video_closed"));

                            } catch (JSONException e) {}
                        }
                    }, null);
                } catch (JSONException e) {}
            }
        }, null);
    }

    class episode_list_adapter extends RecyclerView.Adapter<episode_list_adapter.EpisodeViewHolder>{

        JSONArray episodes;
        int[] playheads;
        int seconds;

        class EpisodeViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView img;
            ProgressBar bar;

            EpisodeViewHolder(View itemView) {
                super(itemView);
                name = (TextView)itemView.findViewById(R.id.episode_name);
                img = itemView.findViewById(R.id.episode_image);
                bar = itemView.findViewById(R.id.episode_bar);
            }
        }
        @Override
        public int getItemCount() {
            return episodes.length();
        }
        @Override
        public EpisodeViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_list_card, viewGroup, false);
            EpisodeViewHolder vh = new EpisodeViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final EpisodeViewHolder vh, final int i) {
            try {

                vh.name.setText(episodes.getJSONObject(i).getString("title"));

                float progress = (float)playheads[i]/(float)seconds;
                progress *= 100;

                vh.bar.setProgress((int)progress);
                Tools.picasso.load(episodes.getJSONObject(i).getString("thumbnail")).into(vh.img);

                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            final NetworkResponse out = new NetworkResponse();
                            NetworkRequest.make(out, NetworkRequest.CR_info, true, false, new Pair[]{Pair.create("media_id", episodes.getJSONObject(i).getString("url").split("\\-")[episodes.getJSONObject(i).getString("url").split("\\-").length - 1]),
                                    Pair.create("fields","media.episode_number,media.playhead,media.url,media.stream_data")}, getActivity(), new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //send data to fullscreen video activity and start
                                        Intent intent = new Intent(getActivity(), fullscreen_video.class);
                                        intent.putExtra("stream", out.obj.getJSONObject("data").getJSONObject("stream_data").getJSONArray("streams").getJSONObject(0).getString("url"));
                                        intent.putExtra("playhead", playheads[i]);
                                        intent.putExtra("index", i);
                                        startActivity(intent);
                                    } catch (JSONException e) {}
                                }
                            }, new Runnable() {
                                @Override
                                public void run() {
                                    Errors.toast(getActivity(), getString(R.string.stream_error));
                                }
                            });
                        } catch (JSONException e) {}
                    }
                });

            } catch (JSONException e) {}
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }

    void Pirate(final Media media) //TODO maybe add a MasterAnime wallpaper fetching function
    { //MA does not accept searches greater than 30 characters
        String search;
        if (media.name.length() >= 30) {
            search = media.name.substring(0, 30).replace("☆"," ");
        } else {
            search = media.name.replace("☆"," ");
        }

        //first search
        final NetworkResponse out = new NetworkResponse();
        NetworkRequest.make(out, NetworkRequest.MA_search, false, false, new Pair[]{Pair.create("search", search)}, getActivity(), new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray results = new JSONArray(out.str);

                    //use the same name-matching algorithm as used in AL_name_to_gelbooru_tag
                    int[] matching_score = new int[results.length()];

                    for (int j = 0; j < results.length(); j++) {
                        try {
                            for (int q = 0; q < results.getJSONObject(j).getString("title").length(); q++) {
                                if (results.getJSONObject(j).getString("title").toCharArray()[q] == media.name.toCharArray()[q]) {
                                    matching_score[j]++;
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException e) { }
                    }
                    int max_index = 0;
                    for (int j = 0; j < matching_score.length; j++) {
                        if (matching_score[max_index] < matching_score[j]) {
                            max_index = j;
                        }
                    }

                    String id = results.getJSONObject(max_index).getString("id");
                    if (!getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("m-"+media.ALid,"NULL").equals("NULL"))
                    {
                        id = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("m-"+media.ALid,"NULL");
                    }

                    if (id.equals("-1"))
                    {
                        Errors.toast(getActivity(),getString(R.string.could_not_find));
                    } else {
                        //then
                        final NetworkResponse EP_out = new NetworkResponse();
                        NetworkRequest.make(EP_out, NetworkRequest.MA_details, false, false, new Pair[]{Pair.create("", id)}, getActivity(), new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray episodes = EP_out.obj.getJSONArray("episodes");

                                    Dialog episodes_diag = new Dialog(getActivity());
                                    episodes_diag.setContentView(R.layout.episode_list_dialog);
                                    episodes_diag.setCancelable(true);
                                    episodes_diag.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                    RecyclerView ep_list = episodes_diag.findViewById(R.id.episode_list);
                                    LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                                    ep_list.setLayoutManager(llm);

                                    final pirate_episode_list_adapter adapter = new pirate_episode_list_adapter();
                                    adapter.episodes = episodes;
                                    adapter.media = media;
                                    adapter.slug = EP_out.obj.getJSONObject("info").getString("slug");
                                    ep_list.setAdapter(adapter);

                                    episodes_diag.show();

                                } catch (JSONException e) {
                                }
                            }
                        }, null);
                    }
                } catch (JSONException e) {
                    Errors.toast(getActivity(),getString(R.string.could_not_find));
                }
            }
        }, null);
    }

    class pirate_episode_list_adapter extends RecyclerView.Adapter<pirate_episode_list_adapter.EpisodeViewHolder>{

        String slug = "";
        JSONArray episodes;
        Media media;

        class EpisodeViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView img;
            ProgressBar bar;

            EpisodeViewHolder(View itemView) {
                super(itemView);
                name = (TextView)itemView.findViewById(R.id.episode_name);
                img = itemView.findViewById(R.id.episode_image);
                bar = itemView.findViewById(R.id.episode_bar);
            }
        }
        @Override
        public int getItemCount() {
            return episodes.length();
        }
        @Override
        public EpisodeViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_list_card, viewGroup, false);
            EpisodeViewHolder vh = new EpisodeViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final EpisodeViewHolder vh, final int i) {
            try {
                if (episodes.getJSONObject(i).getJSONObject("info").getInt("episode") <= media.progress)
                {
                    vh.bar.setProgress(100);
                }
                else
                {
                    vh.bar.setProgress(0);
                }

                if (episodes.getJSONObject(i).getJSONObject("info").getString("title").equals("null")) {
                    vh.name.setText(getString(R.string.episode)+" "+ episodes.getJSONObject(i).getJSONObject("info").getString("episode"));
                }
                else
                {
                    vh.name.setText(getString(R.string.episode)+" "+ episodes.getJSONObject(i).getJSONObject("info").getString("episode") +" - "+episodes.getJSONObject(i).getJSONObject("info").getString("title"));
                }

                Tools.picasso.load(getString(R.string.MA_episode_thumbnail)+episodes.getJSONObject(i).getString("thumbnail")).into(vh.img);

                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            final NetworkResponse mirrors_out = new NetworkResponse();
                            NetworkRequest.make(mirrors_out, NetworkRequest.MA_episode, false, false, new Pair[]{Pair.create("", slug + "/" + episodes.getJSONObject(i).getJSONObject("info").getInt("episode"))}, getActivity(), new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Document doc = Jsoup.parse(mirrors_out.str);
                                        Element mir = doc.getElementsByTag("video-mirrors").first();

                                        JSONArray mirrors = new JSONArray(mir.attributes().get(":mirrors"));

                                        final Dialog m_d = new Dialog(getActivity());
                                        m_d.setCancelable(true);
                                        m_d.setContentView(R.layout.mirror_list_dialog);
                                        m_d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                        final RecyclerView m_list = m_d.findViewById(R.id.mirror_list);
                                        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                                        m_list.setLayoutManager(llm);
                                        final mirror_list_adapter adapter = new mirror_list_adapter();
                                        adapter.in = media;
                                        adapter.mirrors = mirrors;
                                        adapter.d = m_d;
                                        adapter.adapter = pirate_episode_list_adapter.this;
                                        adapter.number = episodes.getJSONObject(i).getJSONObject("info").getInt("episode");
                                        m_list.setAdapter(adapter);

                                        m_d.show();
                                    } catch (Exception e) {Errors.toast(getActivity(),"LOCAL HOST!");}
                                }
                            }, null);
                        } catch (JSONException e) {}
                    }
                });

            } catch (JSONException e) {}
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }

    class mirror_list_adapter extends RecyclerView.Adapter<mirror_list_adapter.MirrorViewHolder>{

        JSONArray mirrors;
        Media in;
        Dialog d;
        int number;
        pirate_episode_list_adapter adapter;

        class MirrorViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView quality;

            MirrorViewHolder(View itemView) {
                super(itemView);
                name = (TextView)itemView.findViewById(R.id.mirror_name);
                quality = itemView.findViewById(R.id.mirror_quality);
            }
        }
        @Override
        public int getItemCount() {
            return mirrors.length();
        }
        @Override
        public MirrorViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.mirror_list_card, viewGroup, false);
            MirrorViewHolder vh = new MirrorViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final MirrorViewHolder vh, final int i) {
            try {
               vh.name.setText(mirrors.getJSONObject(i).getJSONObject("host").getString("name"));
               vh.quality.setText(mirrors.getJSONObject(i).getString("quality")+"p");

                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            String url = mirrors.getJSONObject(i).getJSONObject("host").getString("embed_prefix")+mirrors.getJSONObject(i).getString("embed_id");
                            if (!mirrors.getJSONObject(i).getJSONObject("host").getString("embed_suffix").equals("null")){ url += mirrors.getJSONObject(i).getJSONObject("host").getString("embed_suffix");}

                            Intent i = new Intent(getActivity(), fullscreen_chrome.class);
                            i.putExtra("url", url);
                            startActivity(i);

                            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.finished_episode));
                            alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int j) {
                                    d.dismiss();
                                    in.progress = number;
                                    adapter.notifyDataSetChanged();
                                    Calendar cal = new GregorianCalendar();
                                    //if this was the first or 0th episode, set start date as today
                                    if (in.progress == 1) {
                                        in.started_year = cal.get(Calendar.YEAR);
                                        in.started_month = cal.get(Calendar.MONTH)+1;
                                        in.started_day = cal.get(Calendar.DAY_OF_MONTH);
                                    }

                                    try {
                                        //if this was the last episode, set end day as today
                                        if (in.progress == Integer.parseInt(in.numEpisodes)) {
                                            in.ended_year = cal.get(Calendar.YEAR);
                                            in.ended_month = cal.get(Calendar.MONTH) + 1;
                                            in.ended_day = cal.get(Calendar.DAY_OF_MONTH);
                                        }
                                    } catch (Exception e) {}

                                    if (getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getBoolean("sync_prog_a", true))
                                    {
                                        final NetworkResponse out = new NetworkResponse();
                                        NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, new Pair[]{Pair.create("query", "mutation {\n" +
                                                "  SaveMediaListEntry(mediaId: " + in.ALid + ", \n" +
                                                "    score: " + in.p_score + ", \n" +
                                                "    progress:" + in.progress + ", \n" +
                                                "    repeat:" + in.repeat + ",\n" +
                                                "    startedAt:{\n" +
                                                "      year:" + in.started_year + ",\n" +
                                                "      month:" + in.started_month + ",\n" +
                                                "      day:" + in.started_day + "\n" +
                                                "    },\n" +
                                                "    completedAt:{\n" +
                                                "      year:" + in.ended_year + ",\n" +
                                                "      month:" + in.ended_month + ",\n" +
                                                "      day:" + in.ended_day + "\n" +
                                                "    }) {\n" +
                                                "    id\n" +
                                                "  }\n" +
                                                "}")}, getActivity(), new Runnable() {
                                            @Override
                                            public void run() {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        DB.DAO().update(in);
                                                        media = DB.DAO().fetch_all();
                                                    }
                                                }).start();
                                            }
                                        }, null);
                                    }
                                }
                            });
                            alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) { //do nothing, just dismiss alertdialog
                                }
                            });

                            alert.show();

                        } catch (JSONException e) {}
                    }
                });

            } catch (JSONException e) {}
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }
}
