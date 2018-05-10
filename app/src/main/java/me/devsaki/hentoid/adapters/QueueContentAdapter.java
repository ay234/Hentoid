package me.devsaki.hentoid.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.database.HentoidDB;
import me.devsaki.hentoid.database.domains.Attribute;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.AttributeType;
import me.devsaki.hentoid.enums.StatusContent;
import me.devsaki.hentoid.events.DownloadEvent;
import me.devsaki.hentoid.util.FileHelper;
import me.devsaki.hentoid.util.Helper;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

/**
 * Created by neko on 11/05/2015.
 * Builds and assigns content from db into adapter for display in queue fragment
 */
public class QueueContentAdapter extends ArrayAdapter<Content> {

    private final Context context;
    private final List<Content> contents;

    public QueueContentAdapter(Context context, List<Content> contents) {
        super(context, R.layout.item_queue, contents);
        this.context = context;
        this.contents = contents;
    }

    @NonNull
    @Override
    public View getView(int pos, View view, @NonNull ViewGroup parent) {
        View v = view;
        ViewHolder holder;
        // Check if an existing view is being reused, otherwise inflate the view
        if (v == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.item_queue, parent, false);

            holder.tvTitle = v.findViewById(R.id.tvTitle);
            holder.ivCover = v.findViewById(R.id.ivCover);
            holder.tvSeries = v.findViewById(R.id.tvSeries);
            holder.tvArtist = v.findViewById(R.id.tvArtist);
            holder.tvTags = v.findViewById(R.id.tvTags);
            holder.tvSite = v.findViewById(R.id.tvSite);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        // Populate the data into the template view using the data object
        // Get the data item for this position
        final Content content = getItem(pos);
        if (content != null) {
            populateLayout(holder, content);
            attachButtons(v, content, (0 == pos), (getCount() - 1 == pos));
            updateProgress(v, content);
        }
        // Return the completed view to render on screen
        return v;
    }

    private void populateLayout(ViewHolder holder, Content content) {
        attachTitle(holder, content);
        attachCover(holder, content);
        attachSeries(holder, content);
        attachArtist(holder, content);
        attachTags(holder, content);

        holder.tvSite.setText(content.getSite().getDescription());
    }

    private void attachTitle(ViewHolder holder, Content content) {
        if (content.getTitle() == null) {
            holder.tvTitle.setText(R.string.work_untitled);
        } else {
            holder.tvTitle.setText(content.getTitle());
            holder.tvTitle.setSelected(true);
        }
    }

    private void attachCover(ViewHolder holder, Content content) {
        RequestBuilder<Drawable> thumb = Glide.with(context).load(content.getCoverImageUrl());

        String coverFile = FileHelper.getThumb(content);
        holder.ivCover.layout(0, 0, 0, 0);

        RequestOptions myOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder);

        Glide.with(context)
                .load(coverFile)
                .apply(myOptions)
                .transition(withCrossFade())
                .thumbnail(thumb)
                .into(holder.ivCover);
    }

    private void attachSeries(ViewHolder holder, Content content) {
        String templateSeries = context.getString(R.string.work_series);
        StringBuilder series = new StringBuilder();
        List<Attribute> seriesAttributes = content.getAttributes().get(AttributeType.SERIE);
        if (seriesAttributes == null) {
            holder.tvSeries.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < seriesAttributes.size(); i++) {
                Attribute attribute = seriesAttributes.get(i);
                series.append(attribute.getName());
                if (i != seriesAttributes.size() - 1) {
                    series.append(", ");
                }
            }
            holder.tvSeries.setVisibility(View.VISIBLE);
        }
        holder.tvSeries.setText(Helper.fromHtml(templateSeries.replace("@series@", series)));

        if (seriesAttributes == null) {
            holder.tvSeries.setText(Helper.fromHtml(templateSeries.replace("@series@",
                    context.getResources().getString(R.string.work_untitled))));
            holder.tvSeries.setVisibility(View.VISIBLE);
        }
    }

    private void attachArtist(ViewHolder holder, Content content) {
        String templateArtist = context.getString(R.string.work_artist);
        StringBuilder artists = new StringBuilder();
        List<Attribute> artistAttributes = content.getAttributes().get(AttributeType.ARTIST);
        if (artistAttributes != null) {
            for (int i = 0; i < artistAttributes.size(); i++) {
                Attribute attribute = artistAttributes.get(i);
                artists.append(attribute.getName());
                if (i != artistAttributes.size() - 1) {
                    artists.append(", ");
                }
            }
        }
        holder.tvArtist.setText(Helper.fromHtml(templateArtist.replace("@artist@", artists)));

        if (artistAttributes == null) {
            holder.tvArtist.setText(Helper.fromHtml(templateArtist.replace("@artist@",
                    context.getResources().getString(R.string.work_untitled))));
            holder.tvArtist.setVisibility(View.VISIBLE);
        }
    }

    private void attachTags(ViewHolder holder, Content content) {
        String templateTags = context.getString(R.string.work_tags);
        StringBuilder tags = new StringBuilder();
        List<Attribute> tagsAttributes = content.getAttributes().get(AttributeType.TAG);
        if (tagsAttributes != null) {
            for (int i = 0; i < tagsAttributes.size(); i++) {
                Attribute attribute = tagsAttributes.get(i);
                if (attribute.getName() != null) {
                    tags.append(templateTags.replace("@tag@", attribute.getName()));
                    if (i != tagsAttributes.size() - 1) {
                        tags.append(", ");
                    }
                }
            }
        }
        holder.tvTags.setText(Helper.fromHtml(tags.toString()));
    }

    private void attachButtons(View view, final Content content, boolean isFirstItem, boolean isLastItem) {
        View btnUp = view.findViewById(R.id.queueUpBtn);
        if (isFirstItem) btnUp.setVisibility(View.INVISIBLE);
        btnUp.setOnClickListener(v -> {
            moveUp(content);
            notifyDataSetChanged();
        });
        View btnDown = view.findViewById(R.id.queueDownBtn);
        if (isLastItem) btnDown.setVisibility(View.INVISIBLE);
        btnDown.setOnClickListener(v -> {
            moveDown(content);
            notifyDataSetChanged();
        });
        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> {
            cancel(content);
            notifyDataSetChanged();
        });
    }

    private void updateProgress(View view, Content content) {
        ProgressBar pb = view.findViewById(R.id.pbDownload);

        if (content.getStatus() != StatusContent.PAUSED) {
            pb.setVisibility(View.VISIBLE);
            if (content.getPercent() > 0) {
                pb.setIndeterminate(false);
                pb.setProgress((int) content.getPercent());
            } else {
                pb.setIndeterminate(true);
            }
        } else {
            pb.setVisibility(View.INVISIBLE);
        }
    }

    private void moveUp(Content content) {
        HentoidDB db = HentoidDB.getInstance(context);
        List<Pair<Integer,Integer>> queue = db.selectQueue();

        int prevItemId = 0;
        int prevItemQueuePosition = -1;
        int prevItemPosition = -1;
        int loopPosition = 0;

        for (Pair<Integer,Integer> p : queue)
        {
            if (p.first.equals(content.getId()) && prevItemId != 0)
            {
                db.udpateQueue(p.first, prevItemQueuePosition);
                db.udpateQueue(prevItemId, p.second);
                Collections.swap(contents, prevItemPosition, loopPosition);
                if (1 == loopPosition) EventBus.getDefault().post(new DownloadEvent(DownloadEvent.EV_SKIP));
                break;
            } else {
                prevItemId = p.first;
                prevItemQueuePosition = p.second;
                prevItemPosition = loopPosition;
            }
            loopPosition++;
        }

        notifyDataSetChanged();
    }

    private void moveDown(Content content) {
        HentoidDB db = HentoidDB.getInstance(context);
        List<Pair<Integer,Integer>> queue = db.selectQueue();

        int itemId = 0;
        int itemQueuePosition = -1;
        int itemPosition = -1;
        int loopPosition = 0;

        for (Pair<Integer,Integer> p : queue)
        {
            if (p.first.equals(content.getId()))
            {
                itemId = p.first;
                itemQueuePosition = p.second;
                itemPosition = loopPosition;
            }
            else if (itemId != 0)
            {
                db.udpateQueue(p.first, itemQueuePosition);
                db.udpateQueue(itemId, p.second);
                Collections.swap(contents, itemPosition, loopPosition);
                if (0 == loopPosition) EventBus.getDefault().post(new DownloadEvent(DownloadEvent.EV_SKIP));
                break;
            }
            loopPosition++;
        }

        notifyDataSetChanged();
    }

    private void cancel(Content content) {
        // Remove content altogether from the DB (including queue)
        HentoidDB db = HentoidDB.getInstance(context);
        db.deleteContent(content);
        // Remove the content from the disk
        FileHelper.removeContent(content);
        // Remove the content from the in-memory list and the UI
        remove(content);

        EventBus.getDefault().post(new DownloadEvent(content, DownloadEvent.EV_CANCEL));
    }

    // View lookup cache
    private static class ViewHolder {
        TextView tvTitle;
        ImageView ivCover;
        TextView tvSeries;
        TextView tvArtist;
        TextView tvTags;
        TextView tvSite;
    }
}
