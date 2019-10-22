package sg.edu.ntu.scse.cz2006.gymbuddies.ui.bdlist;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;

import sg.edu.ntu.scse.cz2006.gymbuddies.AppConstants;
import sg.edu.ntu.scse.cz2006.gymbuddies.ChatActivity;
import sg.edu.ntu.scse.cz2006.gymbuddies.MainActivity;
import sg.edu.ntu.scse.cz2006.gymbuddies.R;
import sg.edu.ntu.scse.cz2006.gymbuddies.adapter.BuddyResultAdapter;
import sg.edu.ntu.scse.cz2006.gymbuddies.datastruct.FavBuddyRecord;
import sg.edu.ntu.scse.cz2006.gymbuddies.datastruct.User;
import sg.edu.ntu.scse.cz2006.gymbuddies.listener.OnRecyclerViewClickedListener;
import sg.edu.ntu.scse.cz2006.gymbuddies.util.DialogHelper;
import sg.edu.ntu.scse.cz2006.gymbuddies.util.GymHelper;

public class BuddyListFragment extends Fragment implements AppConstants, OnRecyclerViewClickedListener<BuddyResultAdapter.ViewHolder> {
    private final String TAG = "GB.frag.BdList";
    private BuddyListViewModel buddyListViewModel;
    private RecyclerView rvResult;
    private BuddyResultAdapter adapter;
    SwipeRefreshLayout srlUpdateFav;

    private ArrayList<User> listFavUsers;
    private ArrayList<String> listFavUserIds;

    private FirebaseFirestore firestore;
    private DocumentReference favBuddiesRef;
    private FavBuddyRecord favRecord;
    private ListenerRegistration favRecordChangeListener;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        buddyListViewModel = ViewModelProviders.of(this).get(BuddyListViewModel.class);
        View root = inflater.inflate(R.layout.fragment_buddy_list, container, false);

        srlUpdateFav = root.findViewById(R.id.srl_update_fav);
        rvResult = root.findViewById(R.id.rv_buddies);

        if (getActivity() != null) {
            MainActivity activity = (MainActivity) getActivity();
            activity.fab.hide();
        }


//        final TextView textView = root.findViewById(R.id.text_gallery);
        buddyListViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
//                textView.setText(s);
            }
        });


        listFavUsers = new ArrayList<>();
        listFavUserIds = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        adapter = new BuddyResultAdapter(listFavUsers, listFavUserIds);
        adapter.setOnRecyclerViewClickedListener(this);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setOrientation(RecyclerView.VERTICAL);
        rvResult.setAdapter(adapter);
        rvResult.setLayoutManager(mLayoutManager);


        srlUpdateFav.setColorSchemeResources(R.color.google_1, R.color.google_2, R.color.google_3, R.color.google_4);
        srlUpdateFav.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                readData();
            }
        });
        return root;
    }

    private void readData() {
        Log.d(TAG, "do read data");
//        favBuddyHelper.getFavBuddyRecord();
        queryFavUserRecord();
        srlUpdateFav.setRefreshing(true);
    }

    private void queryFavUserRecord() {
        Log.d(TAG, "queryFavRecord");
        if (favBuddiesRef == null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            favBuddiesRef = firestore.collection(COLLECTION_FAV_BUDDY).document(uid);
        }

        favBuddiesRef.get().addOnSuccessListener((documentSnapshot) -> {
            Log.d(TAG, "favBuddiesRef.get() -> onSuccess " + documentSnapshot);
            readFavRecordDoc(documentSnapshot);
        }).addOnFailureListener((e) -> {
            Log.d(TAG, "favBuddiesRef.get() -> onFailed " + e.getMessage());
        });
    }

    private void readFavRecordDoc(DocumentSnapshot documentSnapshot) {
        favRecord = documentSnapshot.toObject(FavBuddyRecord.class);
        if (favRecord == null) {
            favRecord = new FavBuddyRecord();
        }
        listFavUserIds.clear();
        listFavUserIds.addAll(favRecord.getBuddiesId());

        queryBuddies();
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");
        readData();
    }

    @Override
    public void onResume() {
        super.onResume();

//        listenFavRecordChanges();
    }

    @Override
    public void onPause() {

//        stopListenFavRecordChanges();
        super.onPause();
    }
//    private void listenFavRecordChanges(){
//        favRecordChangeListener = favBuddiesRef.addSnapshotListener((doc, e)->{
//            Log.d(TAG, "favBuddiesRef.addSnapshotListener -> onEvent ");
//            if (e != null){
//                Log.w(TAG, "Listen failed", e);
//                return;
//            }
//            readFavRecordDoc(doc);
//        });
//    }
//    private void stopListenFavRecordChanges(){
//        if (favRecordChangeListener!=null){
//            favRecordChangeListener.remove();
//            favRecordChangeListener = null;
//        }
//    }


    private void queryBuddies() {
        Log.d(TAG, "do read buddies");
        CollectionReference userRef = firestore.collection(GymHelper.GYM_USERS_COLLECTION);
        userRef.get().addOnSuccessListener((snapshots) -> {
            Log.d(TAG, "userRef.get() success");
            readUserQuerySnapshot(snapshots);
        }).addOnFailureListener((e) -> {
            Log.d(TAG, "query all users failed");
        });
    }

    private void readUserQuerySnapshot(QuerySnapshot snapshots) {
        Log.d(TAG, "readUserQuerySnapshot -> " + snapshots);
        ArrayList<User> users = new ArrayList<>();
        users.addAll(snapshots.toObjects(User.class));
        Log.d(TAG, "user.size->" + users.size());
        Log.d(TAG, "favUserIds.size->" + listFavUserIds.size());

        // perform filtering
        listFavUsers.clear();
        String curUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        for (User user : users) {
            if (user.getUid().equals(curUserId)) {
                continue;
            }
            if (listFavUserIds.contains(user.getUid())) {
                listFavUsers.add(user);
            }
        }

        adapter.notifyDataSetChanged();
        Log.d(TAG, "fav user.size->" + listFavUsers.size());
        if (srlUpdateFav.isRefreshing()) {
            srlUpdateFav.setRefreshing(false);
        }
    }

    @Override
    public void onViewClicked(View view, BuddyResultAdapter.ViewHolder holder, int action) {
        User user = listFavUsers.get(holder.getAdapterPosition());
        switch (action) {
            case BuddyResultAdapter.ACTION_CLICK_ON_FAV_ITEM:
                unfavUser(user);
                break;

            case BuddyResultAdapter.ACTION_CLICK_ON_ITEM_BODY:
                goChatActivity(user);
                break;

            case BuddyResultAdapter.ACTION_CLICK_ON_ITEM_PIC:
                DialogHelper.displayBuddyProfile(getContext(), user, ((ImageView) view).getDrawable());
                break;
            default:
        }
    }


    private void unfavUser(User other) {
        listFavUsers.remove(other);
        listFavUserIds.remove(other.getUid());
        favRecord.getBuddiesId().remove(other.getUid());
        commitFavRecord();
        adapter.notifyDataSetChanged();
        Snackbar snackbar = Snackbar.make(rvResult, R.string.txt_msg_removed_favourite, Snackbar.LENGTH_SHORT);
        snackbar.setAction(R.string.txt_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listFavUsers.add(other);
                listFavUserIds.add(other.getUid());
                favRecord.getBuddiesId().add(other.getUid());
                commitFavRecord();
                adapter.notifyDataSetChanged();
                Snackbar.make(rvResult, R.string.txt_msg_removed_favtorite_undone, Snackbar.LENGTH_SHORT).show();

            }
        });
        snackbar.show();
    }

    private void commitFavRecord() {
        firestore.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                transaction.set(favBuddiesRef, favRecord);
                return null;
            }
        }).addOnSuccessListener((v) -> {
            Log.d(TAG, "favRecord updated success");
        }).addOnFailureListener((e) -> {
            Log.d(TAG, "favRecord updated failed");
            e.printStackTrace();
        });
    }

    private void goChatActivity(User other) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        Bundle data = new Bundle();
        data.putString("buddy_id", other.getUid());
        data.putString("buddy_name", other.getName());
        data.putString("buddy_pic_url", other.getProfilePicUri());
        intent.putExtras(data);
        startActivity(intent);
    }


}