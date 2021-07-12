package net.k1ra.kirino;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.security.KeyPairGeneratorSpec;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

import static android.app.Activity.RESULT_OK;

public class frag_settings extends Fragment {
    boolean showed = false;

    public static Fragment newInstance() {
        return new frag_settings();
    }

    int PICK_IMAGE_REQUEST = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_settings, container, false);

        //if token is acquired, go to AniList settings
        if (getActivity().getIntent().getDataString() != null && !getActivity().getIntent().getDataString().split("=")[1].split("&")[0].equals("access_denied") && !showed &&
                !getActivity().getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[1].equals("media") &&
                !getActivity().getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[1].equals("chara"))
        {
            AniList_settings();
            showed = true;
        }

        final Button AniList = rootView.findViewById(R.id.set_anilist);
        final Button reminders = rootView.findViewById(R.id.set_reminder);
        final Button refresh = rootView.findViewById(R.id.set_refresh);
        final Button fanart = rootView.findViewById(R.id.set_fanart);
        final Button CR = rootView.findViewById(R.id.set_CR);
        final Button piracy = rootView.findViewById(R.id.set_piracy);
        final Button background = rootView.findViewById(R.id.set_background);
        final Button donate = rootView.findViewById(R.id.donate);
        final Button about = rootView.findViewById(R.id.set_about);

        donate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Loading.show(getActivity());
                final BillingClient mBillingClient = BillingClient.newBuilder(getActivity()).setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@BillingClient.BillingResponse int responseCode, List purchases) {
                        if (responseCode == BillingClient.BillingResponse.OK
                                && purchases != null) {
                            Errors.toast(getActivity(), getText(R.string.donate_done));
                        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
                            Errors.toast(getActivity(), getText(R.string.donate_cancelled));
                        } else {
                            Errors.toast(getActivity(), getText(R.string.donate_error));
                        }
                    }
                }).build();
                mBillingClient.startConnection(new BillingClientStateListener() {
                    @Override
                    public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                        if (billingResponseCode == BillingClient.BillingResponse.OK) {
                            try {
                                final Dialog settings = new Dialog(getActivity());
                                settings.setCancelable(true);
                                settings.setContentView(R.layout.dialog_donate);
                                settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                                final Spinner amount = settings.findViewById(R.id.donate_spinner);
                                final Button go = settings.findViewById(R.id.btn_donate);

                                final List<String> prices = new ArrayList<String>();
                                final List<String> sku_id = new ArrayList<String>();
                                final List skuList = new ArrayList<>();
                                skuList.add("k1ra.kirino.doante.1");
                                skuList.add("k1ra.kirino.doante.2");
                                skuList.add("k1ra.kirino.doante.3");
                                skuList.add("k1ra.kirino.doante.5");
                                skuList.add("k1ra.kirino.doante.10");
                                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                                mBillingClient.querySkuDetailsAsync(params.build(),
                                        new SkuDetailsResponseListener() {
                                            @Override
                                            public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
                                                for (SkuDetails skuDetails : skuDetailsList) {
                                                    prices.add(skuDetails.getPrice());
                                                    sku_id.add(skuDetails.getSku());
                                                }
                                                Loading.dismiss();

                                                amount.setAdapter(new ArrayAdapter<String>(getActivity(),
                                                        android.R.layout.simple_spinner_item, prices));
                                            }
                                        });

                                go.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        try {
                                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                                    .setSku(sku_id.get(amount.getSelectedItemPosition()).toString())
                                                    .setType(BillingClient.SkuType.INAPP) // SkuType.SUB for subscription
                                                    .build();
                                            int responseCode = mBillingClient.launchBillingFlow(getActivity(), flowParams);
                                        } catch (Exception e) {
                                        }
                                    }
                                });

                                settings.show();
                            } catch (Exception e) {}
                        }
                    }
                    @Override
                    public void onBillingServiceDisconnected() {
                        Errors.no_internet_toast(getActivity());
                    }
                });
            }
        });

        AniList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AniList_settings();
            }
        });
        reminders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
                final Dialog settings = new Dialog(getActivity());
                settings.setCancelable(true);
                settings.setContentView(R.layout.dialog_settings_reminder);
                settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                final Switch rem = settings.findViewById(R.id.rem_sw_new);
                rem.setChecked(sharedPref.getBoolean("rem", true));
                rem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sharedPref.edit().putBoolean("rem", rem.isChecked()).commit();
                    }
                });

                settings.show();
            }
        });
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
                final Dialog settings = new Dialog(getActivity());
                settings.setCancelable(true);
                settings.setContentView(R.layout.dialog_settings_refresh);
                settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                final EditText value = settings.findViewById(R.id.ref_et);
                final Button save = settings.findViewById(R.id.ref_save);

                value.setText(String.valueOf(sharedPref.getInt("sync_wait", 15)));

                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            sharedPref.edit().putInt("sync_wait", Integer.parseInt(value.getText().toString())).commit();
                        } catch (ParseException e) {} //don't be a cheeky big baka, user-chan
                        settings.dismiss();
                    }
                });

                settings.show();
            }
        });
        fanart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
                final Dialog settings = new Dialog(getActivity());
                settings.setCancelable(true);
                settings.setContentView(R.layout.dialog_settings_fanart);
                settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                final EditText tags = settings.findViewById(R.id.fa_global);
                final Button save = settings.findViewById(R.id.fa_save);

                tags.setText(sharedPref.getString("fa_global", getString(R.string.global_tag_default)));

                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sharedPref.edit().putString("fa_global", tags.getText().toString()).commit();
                        settings.dismiss();
                    }
                });

                settings.show();
            }
        });
        CR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);

                if (sharedPref.getBoolean("CR_logged_in", false))
                {
                    final Dialog settings = new Dialog(getActivity());
                    settings.setCancelable(true);
                    settings.setContentView(R.layout.dialog_settings_cr_logged_in);
                    settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                    final TextView text = settings.findViewById(R.id.cr_l_i_text);
                    final Button logout = settings.findViewById(R.id.cr_l_i_logout);

                    text.setText(getString(R.string.CR_logged_in)+" "+sharedPref.getString("CR_email",""));
                    logout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            sharedPref.edit().putBoolean("CR_logged_in", false).commit();
                            settings.dismiss();
                        }
                    });

                    settings.show();
                }
                else
                {
                    AlertDialog.Builder warning = new AlertDialog.Builder(getActivity());
                    warning.setNegativeButton("back", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    warning.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final Dialog settings = new Dialog(getActivity());
                            settings.setCancelable(true);
                            settings.setContentView(R.layout.dialog_settings_cr_login);
                            settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                            final EditText email = settings.findViewById(R.id.cr_l_i_email);
                            final EditText pass = settings.findViewById(R.id.cr_l_i_pass);
                            final Button login = settings.findViewById(R.id.cr_l_i_login);

                            login.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    try {
                                        Loading.show(getActivity());
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Tools.store_CR_credentials(email.getText().toString(), pass.getText().toString(), getActivity());
                                                final NetworkResponse out = new NetworkResponse();
                                                NetworkRequest.make(out, NetworkRequest.CR_login, true, false, new Pair[]{Pair.create("account",sharedPref.getString("CR_email","")), Pair.create("password", Tools.fetch_CR_password(getActivity()))}, getActivity(), new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            //make sure user is premium
                                                            if (out.obj.getJSONObject("data").getJSONObject("user").getString("premium").equals("anime|drama|manga"))
                                                            {
                                                                //start ze session
                                                                final NetworkResponse ss_out = new NetworkResponse();
                                                                NetworkRequest.make(ss_out, NetworkRequest.CR_start_session, true, false, new Pair[]{Pair.create("auth", out.obj.getJSONObject("data").getString("auth"))}, getActivity(), new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        try {
                                                                            //we're logged in!
                                                                            sharedPref.edit().putBoolean("CR_logged_in", true).putString("CR_session_id", ss_out.obj.getJSONObject("data").getString("session_id")).commit();
                                                                            settings.dismiss();

                                                                            CR.callOnClick();
                                                                        } catch (JSONException e) {}
                                                                    }
                                                                }, null);
                                                            } else {
                                                                Errors.toast(getActivity(), getString(R.string.CR_premium_needed));
                                                            }
                                                        } catch (JSONException e) {
                                                            Errors.toast(getActivity(),getString(R.string.login_fail));
                                                        }
                                                    }
                                                }, null);
                                            }
                                        }).start();
                                    } catch (Exception e) { e.printStackTrace();
                                        Errors.toast(getActivity(), getString(R.string.keystore_exception));
                                    }
                                }
                            });

                            settings.show();
                        }
                    });

                    warning.setTitle(R.string.CR_warning_title);
                    warning.setMessage(R.string.CR_warning);
                    warning.show();
                }
            }
        });
        piracy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
                final Dialog settings = new Dialog(getActivity());
                settings.setCancelable(true);
                settings.setContentView(R.layout.dialog_settings_piracy);
                settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                final Switch no_redirect = settings.findViewById(R.id.set_no_redirect);
                no_redirect.setChecked(sharedPref.getBoolean("no_redirect", true));
                no_redirect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sharedPref.edit().putBoolean("no_redirect", no_redirect.isChecked()).commit();
                    }
                });

                settings.show();
            }
        });
        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.set_background)), PICK_IMAGE_REQUEST);
            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog settings = new Dialog(getActivity());
                settings.setCancelable(true);
                settings.setContentView(R.layout.dialog_settings_about);
                settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                final TextView version = settings.findViewById(R.id.VersionString);
                version.setText(BuildConfig.VERSION_NAME);

                settings.show();
            }
        });

        return rootView;
    }

    void AniList_settings()
    {
        final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
        final Dialog settings = new Dialog(getActivity());
        settings.setCancelable(true);
        settings.setContentView(R.layout.dialog_settings_anilist);
        settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        final Button login = settings.findViewById(R.id.set_al_login);
        final Switch sync_a = settings.findViewById(R.id.al_sw_sync_a);
        final Switch sync_m = settings.findViewById(R.id.al_sw_sync_m);
        final Switch sync_w = settings.findViewById(R.id.al_sw_sync_w);
        final Switch sync_prog_a = settings.findViewById(R.id.al_sw_sync_prog_a);
        final Switch sync_prog_m = settings.findViewById(R.id.al_sw_sync_prog_m);

        //if user is not logged in, prompt for login
        if (sharedPref.getString("AL_token", "").equals("")) {
            //handle anilist login in web browser
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.anilist_warning));

                    alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int j) {
                            //dismiss to prevent window leaking
                            settings.dismiss();
                            //launch AL login/auth in browser
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse("https://anilist.co/api/v2/oauth/authorize?client_id=" + getString(R.string.AL_client_id) + "&response_type=token"));
                            startActivity(i);
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

            sync_a.setVisibility(View.GONE);
            sync_m.setVisibility(View.GONE);
            sync_w.setVisibility(View.GONE);
            sync_prog_a.setVisibility(View.GONE);
            sync_prog_m.setVisibility(View.GONE);
        }
        else
        {
            login.setVisibility(View.GONE);

            sync_a.setChecked(sharedPref.getBoolean("sync_a", true));
            sync_m.setChecked(sharedPref.getBoolean("sync_m", true));
            sync_w.setChecked(sharedPref.getBoolean("sync_w", true));
            sync_prog_a.setChecked(sharedPref.getBoolean("sync_prog_a", true));
            sync_prog_m.setChecked(sharedPref.getBoolean("sync_prog_m", true));

            sync_a.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedPref.edit().putBoolean("sync_a", sync_a.isChecked()).commit();
                }
            });
            sync_m.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedPref.edit().putBoolean("sync_m", sync_m.isChecked()).commit();
                }
            });
            sync_w.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedPref.edit().putBoolean("sync_w", sync_w.isChecked()).commit();
                }
            });
            sync_prog_a.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedPref.edit().putBoolean("sync_prog_a", sync_prog_a.isChecked()).commit();
                }
            });
            sync_prog_m.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedPref.edit().putBoolean("sync_prog_m", sync_prog_m.isChecked()).commit();
                }
            });
        }

        settings.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), data.getData());
                String[] projection = { MediaStore.Images.Media.DATA };
                FileOutputStream out = null;
                try {
                    File file = new File(getActivity().getExternalFilesDir(null), "background.png");
                    out = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) { e.printStackTrace();} finally {
                    try {
                        if (out != null) {
                            out.flush();
                            out.close();
                            getActivity().sendBroadcast(new Intent("net.k1ra.background"));
                        }
                    } catch (IOException e) {e.printStackTrace();}
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
