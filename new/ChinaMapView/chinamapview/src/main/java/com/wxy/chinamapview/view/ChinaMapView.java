package com.wxy.chinamapview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

import com.wxy.chinamapview.gesture.MyGestureDector;
import com.wxy.chinamapview.gesture.MyScaleGestureDetector;
import com.wxy.chinamapview.gesture.ScrollScaleGestureDetector;
import com.wxy.chinamapview.model.ChinaMapModel;
import com.wxy.chinamapview.model.ProvinceModel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import static android.content.ContentValues.TAG;

public class ChinaMapView extends View {
    private Paint innerPaint,outerPaint;//画省份的内部画笔和外圈画笔
    private boolean isFirst; //是否是第一次绘制,用于最初的适配
    private ChinaMapModel chinaMapModel;
    private float[] matrixValues = new float[9];
    private int viewWidth;
    private int viewHeight;
    private int mapWidth,mapHeight;//map初始宽度和高度
    private   int DEFUALT_VIEW_WIDTH=1200;//设置默认的宽
    private float map_scale;//初始适配缩放值
    private int selectPosition=-1;
    private int scaleMax=2;//缩放的最大倍数
    private int scaleMin=1;//缩放的最小倍数
    private Matrix myMatrix;    //用来完成缩放
    private ScrollScaleGestureDetector scrollScaleGestureDetector;//自定义的缩放拖拽手势帮助类
    private onProvinceClickLisener onProvinceClickLisener;//省份点击回调
    private MyGestureDector myGestureDector;//拖动惯性滑动点击帮助类
    private ScrollScaleGestureDetector.OnScrollScaleGestureListener onScrollScaleGestureListener=new ScrollScaleGestureDetector.OnScrollScaleGestureListener() {
        @Override
        public void onClick(float x, float y) {
            //只有点击在某一个省份内才会触发省份选择接口
            for (ProvinceModel p:chinaMapModel.getProvinceslist()){
                for (Region region:p.getRegionList()){
                    if (region.contains((int)x, (int)y)){
                        //重置上一次选中省份的状态
                        if (selectPosition!=-1){
                        chinaMapModel.getProvinceslist().get(selectPosition).setSelect(false);
                        }
                        //设置新的选中的省份
                        p.setSelect(true);
                        //暴露到Activity中的接口，把省的名字传过去
                        if (onProvinceClickLisener!=null){
                        onProvinceClickLisener.onSelectProvince(p.getName());
                        }
                        invalidate();
                        return;
                    }
                }
            }

        }
    };
    private MyScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    public void setOnProvinceClickLisener(ChinaMapView.onProvinceClickLisener onProvinceClickLisener) {
        this.onProvinceClickLisener = onProvinceClickLisener;
    }

    public ChinaMapModel getChinaMapModel() {
        return chinaMapModel;
    }

    public void setChinaMapModel(ChinaMapModel chinaMapModel) {
        this.chinaMapModel = chinaMapModel;
        isFirst=true;
        requestLayout();
    }
    public void notifyDataSetChanged(){
        invalidate();
    }
    public ChinaMapView(Context context) {
        this(context,null);
    }

    public ChinaMapView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ChinaMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //初始化省份内部画笔
        innerPaint=new Paint();
        innerPaint.setAntiAlias(true);
        //初始化省份外框画笔
        outerPaint=new Paint();
        outerPaint.setAntiAlias(true);
        outerPaint.setStrokeWidth(1);
        outerPaint.setStyle(Paint.Style.STROKE);
        //初始化手势帮助类
        scrollScaleGestureDetector=new ScrollScaleGestureDetector(this,onScrollScaleGestureListener);
        myMatrix=new Matrix();
        scaleGestureDetector=new MyScaleGestureDetector(context,new MyScaleGestureDetector.OnScaleGestureListener(){
            @Override
            public boolean onScale(MyScaleGestureDetector detector) {
                float scale = getScale();
                float yFactor = detector.getScaleFactor();
                        if (yFactor * scale < scaleMin){
                            yFactor = scaleMin / scale;}
                        if (yFactor * scale > scaleMax){
                            yFactor = scaleMax / scale;}
                myMatrix.postScale(yFactor, yFactor, detector.getFocusX(), detector.getFocusY());
                resetTranslate();
                invalidate();
                return true;
            }
            @Override
            public boolean onScaleBegin(MyScaleGestureDetector detector) {
                return true;
            }
            @Override
            public void onScaleEnd(MyScaleGestureDetector detector) {
            }
        });
        gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDown(MotionEvent e) {
                Log.v("xixi=","onDown");
                return true;
            }
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.v("xixi=","onScroll="+distanceX);
                    myMatrix.postTranslate(-(distanceX),-(distanceY));
                    invalidate();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
            }
        });
        myGestureDector=new MyGestureDector(context,this, myMatrix, new MyGestureDector.OnGestureClickListener() {
            @Override
            public void onClick(int x, int y) {
                //只有点击在某一个省份内才会触发省份选择接口
                for (ProvinceModel p:chinaMapModel.getProvinceslist()){
                    for (Region region:p.getRegionList()){
                        if (region.contains((int)x, (int)y)){
                            //重置上一次选中省份的状态
                            if (selectPosition!=-1)
                            chinaMapModel.getProvinceslist().get(selectPosition).setSelect(false);
                            //设置新的选中的省份
                            p.setSelect(true);
                            //暴露到Activity中的接口，把省的名字传过去
                            onProvinceClickLisener.onSelectProvince(p.getName());
                            invalidate();
                            return;
                        }
                    }
                }
            }
        });
    }
    //重置因缩放导致的边界越界
    private void resetTranslate() {
        if (mapHeight!=0&&mapWidth!=0&&viewHeight!=0&&viewWidth!=0){
            RectF rectF=getMatrixRectF();
            //其实，因为精度问题，只会有这两种情况，(float)viewWidth/(float)viewHeight和(float)mapWidth/(float)mapHeight始终不相等。
            //即view的长宽比和map缩放后的长宽比基本一致的情况，按照下面任何一种情况都可以处理
            // (float)viewWidth/(float)viewHeight+"="+(float)mapWidth/(float)mapHeight);
            float dx=0,dy=0;
            if ((float)viewWidth/(float)viewHeight<(float)mapWidth/(float)mapHeight){
                if (rectF.left>=viewWidth/2){
                    dx=viewWidth/2-rectF.left;
                }
                if (rectF.right<=viewWidth/2){
                    dx=viewWidth/2-rectF.right;
                }
                if (rectF.bottom<=viewHeight-mapHeight/2){
                    dy=viewHeight-mapHeight/2-rectF.bottom;
                }
                if (rectF.top>=viewHeight-mapHeight/2){
                    dy=viewHeight-mapHeight/2-rectF.top;
                }
            }else  {
                if (rectF.top>=viewHeight/2){
                    dy=viewHeight/2-rectF.top;
                }
                if (rectF.bottom<=viewHeight/2){
                    dy=viewHeight/2-rectF.bottom;
                }
                if (rectF.left>=viewWidth-mapWidth/2){
                    dx=viewWidth-mapWidth/2-rectF.left;
                }
                if (rectF.right<=viewWidth-mapWidth/2){
                    dx=viewWidth-mapWidth/2-rectF.right;
                }
            }
            myMatrix.postTranslate(dx,dy);
        }
    }

    //设置最大缩放倍数，最小为1
    public void setScaleMax(int scaleMax) {
        if (scaleMax<=1){
            this.scaleMax=1;
        }else{
            this.scaleMax=scaleMax;
        }
        scrollScaleGestureDetector.setScaleMax(this.scaleMax);//最大缩放倍数
    }
    //设置最小缩放倍数，最小为0
    public void setScaleMin(int scaleMin) {
        if (scaleMin<=0){
            this.scaleMin=0;
        }else{
            this.scaleMin=scaleMin;
        }
        scrollScaleGestureDetector.setScaleMin(this.scaleMin);//最小缩放倍数
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (chinaMapModel==null){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }else {
            int width=0,height=0;
            int width_specMode= MeasureSpec.getMode(widthMeasureSpec);
            int height_specMode= MeasureSpec.getMode(heightMeasureSpec);
            switch (width_specMode){
                //宽度精确值
                case MeasureSpec.EXACTLY:
                        map_scale=(float) MeasureSpec.getSize(widthMeasureSpec)/(chinaMapModel.getMaxX()-chinaMapModel.getMinX());
                    switch (height_specMode){
                        //高度精确值
                        case MeasureSpec.EXACTLY:
                            width= MeasureSpec.getSize(widthMeasureSpec);
                            height= MeasureSpec.getSize(heightMeasureSpec);
                            break;
                        case MeasureSpec.AT_MOST:
                        case MeasureSpec.UNSPECIFIED:
                            width= MeasureSpec.getSize(widthMeasureSpec);
                            if ( getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec)!=0) {
                                height = Math.min((int) ((chinaMapModel.getMaxY() - chinaMapModel.getMinY()) * map_scale), getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
                            }else {
                                height =(int) ((chinaMapModel.getMaxY() - chinaMapModel.getMinY()) * map_scale);
                            }
                            break;
                    }
                    break;
                //宽度wrap_content
                case MeasureSpec.AT_MOST:
                case MeasureSpec.UNSPECIFIED:
                    switch (height_specMode){
                        //高度精确值
                        case MeasureSpec.EXACTLY:
                            map_scale=(float)MeasureSpec.getSize(heightMeasureSpec)/(chinaMapModel.getMaxY()-chinaMapModel.getMinY());
                            height= MeasureSpec.getSize(heightMeasureSpec);
                            if (getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec)!=0) {
                                width = Math.min((int) ((chinaMapModel.getMaxX() - chinaMapModel.getMinX()) * map_scale), getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec));
                            }else {
                                width =(int) (chinaMapModel.getMaxX() - chinaMapModel.getMinX());
                            }
                            break;
                        case MeasureSpec.AT_MOST:
                        case MeasureSpec.UNSPECIFIED:
                            width=DEFUALT_VIEW_WIDTH;
                            map_scale=width/(chinaMapModel.getMaxX()-chinaMapModel.getMinX());
                            height= (int) ((chinaMapModel.getMaxY()-chinaMapModel.getMinY())*map_scale);
                            break;
                    }
                    break;
            }
            setMeasuredDimension(width,height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (chinaMapModel==null)return;
        //保证只在初次绘制的时候进行缩放适配
        if (isFirst){
            viewWidth=getWidth();
            viewHeight=getHeight();
            //提供给滑动帮助类，以便控制滑动边界
            myGestureDector.setViewWidthAndHeight(viewWidth,viewHeight);
            //首先重置所有点的坐标，使得map适应屏幕大小
            if ((float)viewWidth/(float)viewHeight>(chinaMapModel.getMaxX()-chinaMapModel.getMinX())/(chinaMapModel.getMaxY()-
                    chinaMapModel.getMinY())){
                map_scale=viewHeight/(chinaMapModel.getMaxY()-chinaMapModel.getMinY());
            }else {
                map_scale=viewWidth/(chinaMapModel.getMaxX()-chinaMapModel.getMinX());
            }
            //缩放所有Path
            scalePoints(canvas,map_scale);
            isFirst=false;

        }else {
            //关联缩放和平移后的矩阵
            canvas.concat(myMatrix);
            drawMap(canvas);
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
         scaleGestureDetector.onTouchEvent(event);
        if (!scaleGestureDetector.isInProgress()) {
            myGestureDector.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        myGestureDector.computeScroll();
    }

    private void drawMap(Canvas canvas) {
        if (chinaMapModel.getProvinceslist().size()>0){
            outerPaint.setStrokeWidth(1);
            //首先记录下点击的省份的下标，先把其他的省份绘制完，
            for (int i=0;i<chinaMapModel.getProvinceslist().size();i++){
                if (chinaMapModel.getProvinceslist().get(i).isSelect()){
                    selectPosition=i;
                }else{
                    //此时绘制其他省份，边框画笔的宽度为1
                    innerPaint.setColor(chinaMapModel.getProvinceslist().get(i).getColor());
                    outerPaint.setColor(chinaMapModel.getProvinceslist().get(i).getNormalBordercolor());
                    for (Path p:chinaMapModel.getProvinceslist().get(i).getListpath()){
                        canvas.drawPath(p, innerPaint);
                        canvas.drawPath(p, outerPaint);
                    }
                }
            }
            if (selectPosition!=-1){
            //再绘制点击所在的省份,此时画笔宽度设为2.5，以达到着重显示的效果
            innerPaint.setColor(chinaMapModel.getProvinceslist().get(selectPosition).getColor());
            outerPaint.setColor(chinaMapModel.getProvinceslist().get(selectPosition).getSelectBordercolor());
            outerPaint.setStrokeWidth(2.5f);
            for (Path p:chinaMapModel.getProvinceslist().get(selectPosition).getListpath()){
                canvas.drawPath(p, innerPaint);
                canvas.drawPath(p, outerPaint);
            }
        }
        }
    }
    //第一次绘制，缩小map到View指定大小
    private void scalePoints(Canvas canvas,float scale) {
        if (chinaMapModel.getProvinceslist().size()>0)
            //map的左右上下4个临界点
            chinaMapModel.setMaxX(chinaMapModel.getMaxX()*scale);
        chinaMapModel.setMinX(chinaMapModel.getMinX()*scale);
        chinaMapModel.setMaxY(chinaMapModel.getMaxY()*scale);
        chinaMapModel.setMinY(chinaMapModel.getMinY()*scale);
        //为滑动手势设置边界
        mapWidth=(int)( chinaMapModel.getMaxX()-chinaMapModel.getMinX());
        mapHeight=(int)(chinaMapModel.getMaxY()-chinaMapModel.getMinY());
        myGestureDector.setMapWidthAndHeight(mapWidth,mapHeight);
        //重设所有点
        for (ProvinceModel province:chinaMapModel.getProvinceslist()){
            innerPaint.setColor(province.getColor());
            List<Region> regionList=new ArrayList<>();
            List<Path> pathList=new ArrayList<>();
            innerPaint.setColor(province.getColor());
            outerPaint.setColor(province.getNormalBordercolor());
            for (Path p:province.getListpath()){
                //遍历Path中的所有点，重置点的坐标
                Path newpath=resetPath(p, scale, regionList);
                pathList.add(newpath);
                canvas.drawPath(newpath,innerPaint);
                canvas.drawPath(newpath,outerPaint);
            }
            province.setListpath(pathList);
            //判断点是否在path画出的区域内
            province.setRegionList(regionList);
        }
    }
    private Path resetPath(Path path,float scale,List<Region> regionList) {
        List<PointF> list=new ArrayList<>();
        PathMeasure pathmesure=new PathMeasure(path,true);
        float[] s=new float[2];
        //按照缩放倍数重置Path内的所有点
        for (int i=0;i<pathmesure.getLength();i=i+2) {
            pathmesure.getPosTan(i, s, null);
            PointF p=new PointF((int)s[0]*scale,(int)s[1]*scale);
            list.add(p);
        }
        //重绘缩放后的Path
        Path path1=new Path();
        for (int i=0;i<list.size();i++){
            if (i==0){
                path1.moveTo(list.get(i).x,list.get(i).y);
            }else{
                path1.lineTo(list.get(i).x, list.get(i).y);
            }
        }
        path1.close();
        //构造Path对应的Region,用于判断点击的点是否在Path内
        RectF rf=new RectF();
        Region re=new Region();
        path1.computeBounds(rf,true);
        re.setPath(path1,new Region((int)rf.left,(int)rf.top,(int)rf.right,(int)rf.bottom));
        regionList.add(re);
        return path1;
    }
    //选中所点击的省份
    public interface onProvinceClickLisener{
        public void onSelectProvince(String provinceName);
    }
    public  float getScale() {
        myMatrix.getValues(matrixValues);
        if (matrixValues[Matrix.MSCALE_X]==0){
            return 1;
        }else{
            return matrixValues[Matrix.MSCALE_X];
        }
    }
    private RectF getMatrixRectF() {
        RectF rect = new RectF();
        rect.set(0, 0, viewWidth, viewHeight);
        myMatrix.mapRect(rect);
        return rect;
    }
}