package sg.edu.ntu.scse.cz2006.gymbuddies.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import sg.edu.ntu.scse.cz2006.gymbuddies.R;
import sg.edu.ntu.scse.cz2006.gymbuddies.datastruct.FavBuddyRecord;
import sg.edu.ntu.scse.cz2006.gymbuddies.datastruct.User;
import sg.edu.ntu.scse.cz2006.gymbuddies.tasks.GetProfilePicFromFirebaseAuth;
import sg.edu.ntu.scse.cz2006.gymbuddies.util.FavBuddyHelper;


/**
 * Recycler Adapter for Buddy search result
 * For sg.edu.ntu.scse.cz2006.gymbuddies.adapter in Gym Buddies!
 *
 * @author Chia Yu
 * @since 2019-09-28
 * @property listBuddies List<User> The list of all searched users
 * @constructor Creates a adapter for the Buddy Search Result List RecyclerView
 */
public class BuddyResultAdapter extends RecyclerView.Adapter<BuddyResultAdapter.ViewHolder> {
    private String TAG = "GB.Adapter.BuddyResult";
    public static final int ACTION_INVALID = -1;
    public static final int ACTION_CLICK_ON_ITEM_BODY   = 1;
    public static final int ACTION_CLICK_ON_FAV_ITEM    = 2;
    public static final int ACTION_CLICK_ON_ITEM_PIC = 3;
    private FavBuddyHelper favBuddyHelper;

    /**
     * Custom listener to allow activity to listen to user interaction between views
     */
    public interface OnBuddyClickedListener {
        void onBuddyItemClicked(View view, ViewHolder holder, int action);
    }
    private List<User> listBuddies;
    private OnBuddyClickedListener listener;

    /**
     * Constructor to create Recycler Adapter for Buddy search result
     */
    public BuddyResultAdapter(List<User> listBuddies) {
        this.listBuddies = listBuddies;
    }

    public void setFavBuddyHelper(FavBuddyHelper favBuddyHelper){
        this.favBuddyHelper = favBuddyHelper;
        if (this.favBuddyHelper == null){
            return;
        }
        this.favBuddyHelper.setUpdateListener(new FavBuddyHelper.OnFavBuddiesUpdateListener() {
            @Override
            public void onFavBuddiesChanges(FavBuddyRecord record) {
                Log.d(TAG, "onFavBuddiesChanges->"+record);
                BuddyResultAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void onFavBuddiesUpdate(boolean success) {
                Log.d(TAG, "onFavBuddiesUpdate->"+success);
            }
        });
    }


    /**
     * Allow other classes to listen to user interaction via OnBuddyClickedListener
     */
    public void setOnBuddyClickedListener(OnBuddyClickedListener listener){
        this.listener = listener;
    }

    /**
     * get number of items to be display on recycler view
     */
    @Override
    public int getItemCount() {
        return this.listBuddies.size();
    }

    /**
     * render an item view, and assign the view to view holder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.row_buddy, parent, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }


    /**
     * get number of items to be display on recycler view
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.update();
    }


    /**
     * View Holder to hold data displaying data
     *
     * @author Chia Yu
     * @since 2019-09-28
     * @property itemView the view represents the whole item
     * @constructor Creates a adapter for the Buddy Search Result List RecyclerView
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName;
        ImageView imgViewPic, imgViewGender;
        LinearLayout llPrefDays;
        CheckBox cbFav;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_bd_name);
            imgViewPic = itemView.findViewById(R.id.img_bd_pic);
            imgViewGender = itemView.findViewById(R.id.img_bd_gender);
            llPrefDays = itemView.findViewById(R.id.ll_pref_days);
            cbFav = itemView.findViewById(R.id.cb_bd_fav);

            // set up click listener
            super.itemView.setOnClickListener(this);
            imgViewPic.setOnClickListener(this);
            cbFav.setOnClickListener(this);


            // programmingly change profdays
            CheckBox cbDay;
            final float scale = itemView.getContext().getResources().getDisplayMetrics().density;
            int pixels = (int) (36 * scale + 0.5f);
            llPrefDays.getLayoutParams().height = pixels;
            for (int i = 0; i < llPrefDays.getChildCount(); i++) {
                cbDay = (CheckBox) llPrefDays.getChildAt(i);
                cbDay.setText( cbDay.getText().subSequence(0, 1));
                cbDay.setEnabled(false);
                cbDay.setClickable(false);
            }
        }

        /**
         * update item view based on holder's current position
         */
        private void update(){
            User curUser = listBuddies.get(getAdapterPosition());
            tvName.setText(curUser.getName());
            if (curUser.getGender().equals("Male")) {
                imgViewGender.setImageResource(R.drawable.ic_human_male);
            } else {
                imgViewGender.setImageResource(R.drawable.ic_human_female);
            }

            cbFav.setOnClickListener(null);
            cbFav.setChecked(false);
            if (favBuddyHelper != null && favBuddyHelper.getFavBuddyRecord()!=null && favBuddyHelper.getFavBuddyRecord().getBuddiesId().contains(curUser.getUid()) ){
                cbFav.setChecked(true);
            }
            cbFav.setOnClickListener(this);

            updatePrefDays(curUser);
            updateProfilePic(curUser);
        }


        /**
         * update preference workout days based on user
         */
        private  void updatePrefDays(User user){
            ((CheckBox) llPrefDays.getChildAt(0)).setChecked(user.getPrefDay().getMonday());
            ((CheckBox) llPrefDays.getChildAt(1)).setChecked(user.getPrefDay().getTuesday());
            ((CheckBox) llPrefDays.getChildAt(2)).setChecked(user.getPrefDay().getWednesday());
            ((CheckBox) llPrefDays.getChildAt(3)).setChecked(user.getPrefDay().getThursday());
            ((CheckBox) llPrefDays.getChildAt(4)).setChecked(user.getPrefDay().getFriday());
            ((CheckBox) llPrefDays.getChildAt(5)).setChecked(user.getPrefDay().getSaturday());
            ((CheckBox) llPrefDays.getChildAt(6)).setChecked(user.getPrefDay().getSunday());
        }

        /**
         * update profile picture for each user
         * @see GetProfilePicFromFirebaseAuth
         */
        private void updateProfilePic(User user){
            // TODO: use default picture before picture is being loaded
            // cache image if needed
            if (user.getProfilePicUri() != null) {
                Activity activity = (Activity) itemView.getContext();
                new GetProfilePicFromFirebaseAuth(activity, new GetProfilePicFromFirebaseAuth.Callback() {
                    @Override
                    public void onComplete(@Nullable Bitmap bitmap) {
                        if (bitmap != null) {
                            RoundedBitmapDrawable roundBitmap = RoundedBitmapDrawableFactory.create(activity.getResources(), bitmap);
                            roundBitmap.setCircular(true);
                            imgViewPic.setImageDrawable(roundBitmap);
                        }
                    }
                }).execute(Uri.parse(user.getProfilePicUri()));
            }
        }

        @Override
        public void onClick(View view) {
            int action = ACTION_INVALID;
            if (view == super.itemView) {
                action = ACTION_CLICK_ON_ITEM_BODY;
            } else if (view == cbFav){
                action = ACTION_CLICK_ON_FAV_ITEM;
            } else if (view == imgViewPic){
                action = ACTION_CLICK_ON_ITEM_PIC;
            }
            if (action != ACTION_INVALID && listener != null) {
                listener.onBuddyItemClicked(view,this, action);
            }
        }
    }
}
