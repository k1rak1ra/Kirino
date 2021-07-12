package net.k1ra.kirino;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.math.MathUtils;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public abstract class AL_name_to_gelbooru_tag {
    static String tag = "";
    static int i = 0;
    static boolean run = true;
    static List<String> possible_rearrangements = new ArrayList<String>();
    static int possible_rearr_index = 0;

    static String convert(final String name, final String media, final String ALid, final SharedPreferences s) {
        //first take care of manual overrides, for name-to-tag transitions which seemingly make no bloody sense
        if (!s.getString("w-"+ALid,"NULL").equals("NULL"))
        {
            return s.getString("w-"+ALid,"NULL");
        }

        //try and convert character name to tag with the help of danbooru's API (since both boorus use the same tags)

        //stage 1 preparation. More complicated but prevents issues from having double spaces (which was encountered once with anilist data)
        String[] pieces = name.split("\\s+");
        tag = pieces[0];
        for (int i = 1; i < pieces.length; i++) {
            tag += "_" + pieces[i];
        }
        run = true;
        possible_rearr_index = 0;
        possible_rearrangements.clear();

        //3 stages of trying to get a tag:
        //stage 1/ideal case: adding an underscore to 2-part names or straight single-part name is the tag
        //stage 2: reversing the order of first/last name
        //stage 3/pain: single name given, and it's very common and so anime name is included in tag (for example: Emilia-chan's tag is emilia_(re:zero))
        for (i = 0; i < 4; i++) {
            if (!run) {break;}
            final NetworkResponse out = new NetworkResponse();
            NetworkRequest.make(out, NetworkRequest.TagQuery, false, false, new Pair[]{
                    Pair.create("commit", "Search"),
                    Pair.create("page", "0"),
                    Pair.create("search[hide_empty]", "yes"),
                    Pair.create("search[order]", "count"),
                    Pair.create("search[category]", "4"),
                    Pair.create("search[name_matches]", tag)
            }, null, new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray arr = new JSONArray(out.str);
                        //if returned array isn't empty,
                        if (arr.length() != 0 && i != 2) {
                            run = false;
                        } else {
                            switch (i) {
                                //just finished stage 1, now we'll find every possible combination of elements of the waifu's name. There better not be more than 3 elements or this is gonna be bad
                                case 0:
                                    if (possible_rearrangements.size() == 0)
                                    {
                                      Tools.permute(tag.split("_"), 0, possible_rearrangements);
                                    }

                                    tag = possible_rearrangements.get(possible_rearr_index);
                                    break;
                                    //the anime name might be included. We'll get the character's anime name possibilities by appending a wildcard to the request and then trying to match
                                case 1:
                                    tag += "*";
                                    break;
                                //now we try and match. First, we extract the content of the brackets. For now, we'll assume the form will be around the lines of "name_(anime)" and will remove all colons/spaces, etc and do a direct letter-by-letter comparison
                                case 2:
                                    List<String> anime_given = new ArrayList<String>();

                                    if (arr.length() == 0) { break; }

                                    for (int j = 0; j < arr.length(); j++) {
                                        try {
                                            anime_given.add(arr.getJSONObject(j).getString("name").split("\\(")[1].split("\\)")[0].replace("_", "").replace(":", "").replace("!", "").replace("?", ""));
                                        } catch (JSONException | ArrayIndexOutOfBoundsException e) {
                                        } //there will be items not of the form (other character names) and they'll generate an exception. ignore them and move on
                                    }

                                    //for now we'll select the first anime in the list. set to lower case since tags are lower case
                                    String anime_name = new JSONArray(media).getJSONObject(0).getJSONObject("title").getString("romaji").toLowerCase().replace(" ", "").replace(":", "").replace("-", "").replace("!", "").replace("?", "");

                                    //We're assuming at least the first letter matches. Otherwise, we're screwed desu. We'll comtinue matching letter-by-letter until one option remains, scoring by matching letter.
                                    int[] matching_score = new int[anime_given.size()];

                                    for (int j = 0; j < anime_given.size(); j++) {
                                        try {
                                            for (int q = 0; q < anime_given.get(j).length(); q++) {
                                                if (anime_given.get(j).toCharArray()[q] == anime_name.toCharArray()[q]) {
                                                    matching_score[j]++;
                                                }
                                            }
                                        } catch (ArrayIndexOutOfBoundsException e) {
                                        }
                                    }

                                    //Now the option with the highest score is our tag
                                    int max_index = 0;
                                    for (int j = 0; j < matching_score.length; j++) {
                                        if (matching_score[max_index] < matching_score[j]) {
                                            max_index = j;
                                        }
                                    }
                                    try {
                                        tag = arr.getJSONObject(max_index).getString("name");
                                    } catch (JSONException e) {
                                    }
                                    break;
                                case 3:
                                    if (possible_rearr_index == possible_rearrangements.size()-1) { //all hope is lost, or I'm just shit at writing algorithms
                                        tag = "Error: nothing found";
                                    }
                                    else //we try everything again with another combination
                                    {
                                        possible_rearr_index++;
                                        i = -1;
                                    }
                                    break;
                            }
                        }
                    } catch (JSONException e) {}
                }

            }, new Runnable() {
                @Override
                public void run() {
                    run = false;
                    tag = "Error: no internet";
                }
            });
        }
        return tag;
    }
}
