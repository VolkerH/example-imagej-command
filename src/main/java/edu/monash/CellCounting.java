/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package edu.monash;

import bdv.util.BdvFunctions;
import com.sun.org.glassfish.gmbal.ParameterNames;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.*;
import org.scijava.ui.UIService;
import sun.java2d.pipe.AATextRenderer;
import sun.java2d.pipe.Region;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**

 */
@Plugin(type = Command.class, menuPath = "Plugins>Gauss Filtering")
public class CellCounting<T extends RealType<T>> implements Command {
    //
    // Feel free to add more parameters here...
    //

    @Parameter
    private Dataset currentData;

    @Parameter
    private ImageJ ij;


    @Override
    public void run() {
        final Img<T> image = (Img<T>)currentData.getImgPlus();
        System.out.println("blabla");

        RandomAccessibleInterval gaussFiltered = ij.op().filter().gauss(image, 2.0, 2.0 );
        ij.ui().show(gaussFiltered);
        IterableInterval otsuThresholded;
        otsuThresholded = ij.op().threshold().otsu(Views.iterable(gaussFiltered));
        ij.ui().show((otsuThresholded));
        invertBinaryImage(otsuThresholded);
        // Enter image processing code here ...
        ij.ui().show(otsuThresholded);
        RandomAccessibleInterval rai = ij.op().convert().bit(otsuThresholded);
        ImgLabeling ccaResult = ij.op().labeling().cca(rai, ConnectedComponents.StructuringElement.FOUR_CONNECTED);
        LabelRegions<IntegerType> regions = new LabelRegions(ccaResult);
        int numberofObjects = regions.getExistingLabels().size();
        System.out.println(numberofObjects);
        int count=0;
        IntColumn indexColumn = new IntColumn();
        DoubleColumn meanIntensityColumn = new DoubleColumn();

        long[] dimensions = new long[]{
                image.dimension(0),
                image.dimension(1)
        };
        Img<ARGBType>  resultVisulisation = ArrayImgs.argbs(dimensions);
        Random random = new Random();

        for(LabelRegion region : regions){
            long size = region.size();
            System.out.println("Object number" + count + " has size " + size);
            IterableInterval samppledRegion = Regions.sample(region, image);
            RealType measureMeanIntensity = ij.op().stats().mean(samppledRegion);
            System.out.println("Intensity:" + measureMeanIntensity.getRealFloat());

            indexColumn.add(count);
            meanIntensityColumn.add((double) measureMeanIntensity.getRealFloat());

            IterableInterval pixelsResult = Regions.sample(region, resultVisulisation);
            Cursor cursor = pixelsResult.cursor();
            ARGBType randomColor = new ARGBType(random.nextInt());
            while(cursor.hasNext()){
                ARGBType pixel = (ARGBType) cursor.next();
                pixel.set(randomColor);
            }

            count ++;

        }
        Table table = new DefaultGenericTable();
        table.add(indexColumn);
        table.add(meanIntensityColumn);
        table.setColumnHeader(0,"index");
        table.setColumnHeader(1,"mean intensity");

        ij.ui().show(table);
        //ij.ui().show(resultVisulisation);
        BdvFunctions.show(resultVisulisation, "Bild");
    }

    private void invertBinaryImage(IterableInterval input) {
        Cursor cursor = input.cursor();

        while(cursor.hasNext()){
            BitType pixel = (BitType) cursor.next();
            pixel.set(!pixel.get());
        }

    }
    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // ask the user for a file to open
        final File file = new File("C:/Users/Volker/Data/blobs.tif");
        if (file != null) {
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(CellCounting.class, true);
        }
    }

}
