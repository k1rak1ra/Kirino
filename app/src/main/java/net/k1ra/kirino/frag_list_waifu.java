package net.k1ra.kirino;

import android.app.Dialog;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
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
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static android.app.Activity.RESULT_OK;

public class frag_list_waifu extends Fragment {
    character_DB DB;
    character_DB custom_DB;
    List<Character> waifu = new ArrayList<Character>();
    List<Character> custom_waifu = new ArrayList<Character>();
    BroadcastReceiver receiver;
    int PICK_IMAGE_REQUEST = 4;
    ImageView d_img = null;
    Bitmap bmp = null;

    public static Fragment newInstance() {
        return new frag_list_waifu();
    }

    @Override
    public void onDestroyView()
    {
        //dispose of DB connection and receiver
        DB.close();
        custom_DB.close();
        getActivity().unregisterReceiver(receiver);

        super.onDestroyView();
    }

    @Override
    public void onResume()
    {
        getActivity().sendBroadcast(new Intent("net.k1ra.update.waifu"));
        super.onResume();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_recycler, container, false);

        //init DBs
        DB = Room.databaseBuilder(getActivity(),
                character_DB.class, "waifu_db")
                .build();
        custom_DB = Room.databaseBuilder(getActivity(),
                character_DB.class, "custom_waifu_db")
                .build();

        final RecyclerView list = rootView.findViewById(R.id.recycler);
        final TextView empty = rootView.findViewById(R.id.list_empty);
        final waifu_list_adapter adapter = new waifu_list_adapter();
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        list.setLayoutManager(llm);
        empty.setText(getText(R.string.waifu_list_empty));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intnt) {
                Loading.show(getActivity());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        waifu = DB.DAO().fetch_list();
                        custom_waifu = custom_DB.DAO().fetch_list();
                        waifu.addAll(custom_waifu);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Loading.dismiss();
                                if (waifu.size() == 0) { empty.setVisibility(View.VISIBLE); } else {empty.setVisibility(View.INVISIBLE);}
                                list.setAdapter(adapter);
                            }
                        });
                    }
                }).start();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("net.k1ra.update.waifu");
        getActivity().registerReceiver(receiver, filter);

        return rootView;
    }

    class waifu_list_adapter extends RecyclerView.Adapter<waifu_list_adapter.WaifuViewHolder>{

        class WaifuViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name;
            CardView card;
            Button btn;

            WaifuViewHolder(View itemView) {
                super(itemView);
                    img = itemView.findViewById(R.id.w_l_img);
                    name = itemView.findViewById(R.id.w_l_name);
                    card = itemView.findViewById(R.id.a_card_simple);
                    btn = itemView.findViewById(R.id.waifu_custom_add);
            }
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getItemCount() {
            return waifu.size()+1;
        }
        @Override
        public WaifuViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.waifu_card, viewGroup, false);
            WaifuViewHolder vh = new WaifuViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final WaifuViewHolder vh, final int i) {
            if (i < waifu.size()) {
                vh.btn.setVisibility(View.GONE);
                vh.card.setVisibility(View.VISIBLE);

                if (Integer.parseInt(waifu.get(i).ALid) > 0) {
                    Tools.picasso.load(waifu.get(i).image_URL).into(vh.img);
                } else {
                    try {
                        File imgFile = new File(getActivity().getExternalFilesDir(null), waifu.get(i).image_URL);
                        if (imgFile.exists()) {
                            vh.img.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                        }
                    } catch (Exception e) { }
                }

                vh.name.setText(waifu.get(i).name);

                final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);

                //when card is tapped, show menu
                vh.card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final Dialog dialog = new Dialog(getActivity());
                        dialog.setCancelable(true);
                        dialog.setContentView(R.layout.waifu_card_dialog);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                        final Button fanart = dialog.findViewById(R.id.w_c_d_fanart);
                        final Button details = dialog.findViewById(R.id.w_c_d_details);
                        final Button delet_dis = dialog.findViewById(R.id.w_c_d_delete);

                        if (Integer.parseInt(waifu.get(i).ALid) > 0) {
                            //broadcast to feed to fetch details
                            details.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent("net.k1ra.update.feed_add");
                                    intent.putExtra("type", 3);
                                    intent.putExtra("id", waifu.get(i).ALid);
                                    getActivity().sendBroadcast(intent);
                                    dialog.dismiss();
                                }
                            });
                        } else {
                            details.setVisibility(View.GONE);
                        }

                        //ask for confirmation, then delet from list and update
                        delet_dis.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.waifu_del_1) + " " + waifu.get(i).name + " " + getString(R.string.waifu_del_2));

                                alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int j) {
                                        if (Integer.parseInt(waifu.get(i).ALid) > 0) {
                                            if (sharedPref.getBoolean("sync_w", true) && !sharedPref.getString("AL_token", "").equals("")) {
                                                final NetworkResponse out = new NetworkResponse();
                                                NetworkRequest.make(out, NetworkRequest.AniList.noStrict(), true, false, new Pair[]{Pair.create("query", "mutation\n" +
                                                        "{\n" +
                                                        "  ToggleFavourite(characterId:" + waifu.get(i).ALid + ")\n" +
                                                        "  {\n" +
                                                        "    characters {\n" +
                                                        "     pageInfo {\n" +
                                                        "       total\n" +
                                                        "     }\n" +
                                                        "    }\n" +
                                                        "  }\n" +
                                                        "}")}, getActivity(), new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                DB.DAO().delete(waifu.get(i).ALid);
                                                                getActivity().runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        waifu.remove(i);
                                                                        notifyDataSetChanged();
                                                                        dialog.dismiss();
                                                                    }
                                                                });
                                                            }
                                                        }).start();
                                                    }
                                                }, null);
                                            } else {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        DB.DAO().delete(waifu.get(i).ALid);
                                                        getActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                waifu.remove(i);
                                                                notifyDataSetChanged();
                                                                dialog.dismiss();
                                                            }
                                                        });
                                                    }
                                                }).start();
                                            }
                                        } else {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                custom_DB.DAO().delete(waifu.get(i).ALid);
                                                getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    waifu.remove(i);
                                                    notifyDataSetChanged();
                                                    dialog.dismiss();
                                                }
                                            });
                                                }
                                            }).start();
                                        }
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

                        //fanart request system
                        fanart.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                final Dialog fanart_chooser = new Dialog(getActivity());
                                fanart_chooser.setContentView(R.layout.waifu_card_dialog_fanart);
                                fanart_chooser.setCancelable(true);
                                fanart_chooser.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                final Button gelbooru = fanart_chooser.findViewById(R.id.fa_gel);
                                final EditText tags = fanart_chooser.findViewById(R.id.fa_tags);
                                final EditText num = fanart_chooser.findViewById(R.id.fa_count);
                                final RadioButton safe = fanart_chooser.findViewById(R.id.fa_safe);
                                final RadioButton questionable = fanart_chooser.findViewById(R.id.fa_questionable);
                                final RadioButton explicit = fanart_chooser.findViewById(R.id.fa_explicit);
                                fanart_chooser.show();

                                Loading.show(getActivity());
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Looper.prepare();
                                        final String tag = AL_name_to_gelbooru_tag.convert(waifu.get(i).name, waifu.get(i).media, waifu.get(i).ALid, getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE));
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (tag.equals("Error: no internet")) {
                                                    Errors.no_internet_toast(getActivity());
                                                    fanart_chooser.dismiss();
                                                } else {
                                                    tags.setText(tag);
                                                }
                                                Loading.dismiss();
                                            }
                                        });
                                    }
                                }).start();

                                gelbooru.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        String rating = "safe";
                                        if (questionable.isChecked()) {
                                            rating = "questionable";
                                        } else if (explicit.isChecked()) {
                                            rating = "explicit";
                                        }
                                        final String rating_f = rating;

                                        //first go see how many total posts there are
                                        final NetworkResponse out = new NetworkResponse();
                                        NetworkRequest.make(out, NetworkRequest.Gelbooru.Strict(), false, false, new Pair[]{
                                                Pair.create("limit", "1"),
                                                Pair.create("page", "dapi"),
                                                Pair.create("s", "post"),
                                                Pair.create("q", "index"),
                                                Pair.create("tags", tags.getText().toString() + " rating:" + rating_f + " " + sharedPref.getString("fa_global", getString(R.string.global_tag_default)))
                                        }, getActivity(), new Runnable() {
                                            @Override
                                            public void run() {

                                                try {
                                                    Document output_i = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(out.str.getBytes("utf-8"))));
                                                    output_i.getDocumentElement().normalize();

                                                    Element meta = (Element) output_i.getElementsByTagName("posts").item(0);
                                                    final int total_posts = Integer.parseInt(meta.getAttribute("count"));

                                                    if (total_posts > 0) {
                                                        final int posts_per_page = 100;
                                                        int total_pages = total_posts / posts_per_page;
                                                        final int posts_last_page = total_posts - (total_pages * posts_per_page);
                                                        if (posts_last_page > 0) {
                                                            total_pages++;
                                                        }

                                                        //close dialogs
                                                        fanart_chooser.dismiss();
                                                        dialog.dismiss();

                                                        //500 images max
                                                        if (Integer.parseInt(num.getText().toString().replace(" ", "")) >= 500) {
                                                            num.setText("499");
                                                        }

                                                        int total_getting = Integer.parseInt(num.getText().toString().replace(" ", ""));
                                                        if (total_getting > total_posts) {
                                                            total_getting = total_posts;
                                                        }
                                                        //fetch as much fanart as num or total_posts if less than num
                                                        final int[] post_ids = new int[total_getting];
                                                        final List<FeedItem> posts = new ArrayList<FeedItem>();

                                                        Loading.show(getActivity());
                                                        Tools.fanart_storeall_fetch(total_pages, posts_per_page,
                                                                tags.getText().toString() + " rating:" + rating_f + " " + sharedPref.getString("fa_global", getString(R.string.global_tag_default)), getActivity(), posts, post_ids);
                                                    } else {
                                                        Errors.toast(getActivity(), getString(R.string.no_posts_found));
                                                    }

                                                } catch (ParserConfigurationException | SAXException | IOException e) {
                                                    Errors.no_internet_toast(getActivity());
                                                }
                                            }
                                        }, null);
                                    }
                                });
                            }
                        });

                        dialog.show();
                    }
                });
            } else {
                try {
                    vh.btn.setVisibility(View.VISIBLE);
                    vh.card.setVisibility(View.GONE);

                    vh.btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Errors.toast(getActivity(), getText(R.string.custom_waifu_message));
                            final Dialog form = new Dialog(getActivity());
                            form.setContentView(R.layout.dialog_custom_waifu_add);
                            form.setCancelable(true);
                            form.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                            bmp = null;

                            d_img = form.findViewById(R.id.c_w_img);
                            final EditText name = form.findViewById(R.id.c_w_name);
                            final EditText tags = form.findViewById(R.id.c_w_tags);
                            final Button set_img = form.findViewById(R.id.c_w_btn);
                            final Button save = form.findViewById(R.id.c_w_save);

                            set_img.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent();
                                    intent.setType("image/*");
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent.createChooser(intent, getString(R.string.set_background)), PICK_IMAGE_REQUEST);
                                }
                            });

                            save.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (name.getText().toString().length() > 1 && tags.getText().toString().length() > 1) {
                                        final int number = custom_waifu.size() + 1;
                                        final String n = String.valueOf(-1 * number);
                                        if (bmp != null) {
                                            FileOutputStream out = null;
                                            try {
                                                File file = new File(getActivity().getExternalFilesDir(null), "CUSTOM_" + number + ".png");
                                                out = new FileOutputStream(file);
                                                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            } finally {
                                                try {
                                                    if (out != null) {
                                                        out.flush();
                                                        out.close();
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }

                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit().putString("w-" + n, tags.getText().toString()).commit();
                                                Character c = new Character(n, "CUSTOM_" + number + ".png", name.getText().toString(), "");
                                                custom_DB.DAO().insert_single(c);
                                                waifu.add(c);

                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        notifyDataSetChanged();
                                                        form.dismiss();
                                                    }
                                                });
                                            }
                                        }).start();
                                    } else {
                                        Errors.toast(getActivity(), getString(R.string.c_w_error));
                                    }
                                }
                            });

                            form.show();
                        }
                    });
                } catch (Exception e) {}
            }
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                bmp = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), data.getData());
                d_img.setImageBitmap(bmp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
