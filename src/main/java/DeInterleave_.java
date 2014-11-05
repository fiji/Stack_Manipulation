/* Russell Kincaid, rekincai@syr.edu, November 15, 2002.
 * hacked from Slice_Multiplier.java etc.
 * further modified	by tony collins
 * rewritten 2014-09-19, git@tds.xyz */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

/* This plugin splits slices from time bins in a stack.
 * It deals with the case where stackSize is not evenly divisible by nChannels
 * by dumping the remaining shuffled frames into a new channel, which behavior
 * somebody probably depends on. */

public class DeInterleave_ implements PlugIn {
    int nChannels = (int) Prefs.get("Deint_ch.int", 2);
    boolean keep = Prefs.get("Deint_keep.boolean", true);

    public void run(String arg) {
        ImagePlus inputImage = WindowManager.getCurrentImage();
        if (inputImage == null) {
            IJ.noImage();
            return;
        }

        ImageStack inputStack = inputImage.getStack();
        if (inputStack.getSize() == 1) {
            IJ.error("Please select a stack before choosing Deinterleave");
            return;
        }

        if (!showDialog()) return;

        String inputFilename = inputImage.getTitle();
        Calibration oc = inputImage.getCalibration().copy();

        ImageStack shuffledStack = makeShuffled(inputStack);
        ImagePlus shuffledImage = new ImagePlus(inputFilename, shuffledStack);

        int stackSize = shuffledStack.getSize();
        int nFrames = stackSize/nChannels;

        // the stack is at least nChannels*nFrames large with nFrames timepoints
        for(int channel = 0; channel <= nChannels; channel++) {
            int beginSlice = channel * nFrames;
            int endSlice = Math.min(beginSlice + nFrames, stackSize);
            if(beginSlice == endSlice) break;
            String channelName = Integer.toString(channel+1);
            ImagePlus channelImage = new ImagePlus(inputFilename + " #" + channelName, makeSubStack(shuffledStack, beginSlice, endSlice));
            channelImage.show();
            channelImage.setCalibration(oc);
            channelImage.getWindow().repaint();
        }

        shuffledImage.changes = false;
        shuffledImage.close();
        if (!keep) {
            inputImage.changes = false;
            inputImage.getWindow().close();
        }
        IJ.register(DeInterleave_.class);
    }

    public boolean showDialog() {
        GenericDialog gd = new GenericDialog("De-Interleaver");
        gd.addNumericField("How many channels?", nChannels, 0);
        gd.addCheckbox("Keep source stack", keep);
        gd.showDialog();
        if (gd.wasCanceled()) return false;

        nChannels = (int) gd.getNextNumber();
        keep = gd.getNextBoolean();
        Prefs.set("Deint_ch.int", nChannels);
        Prefs.set("Deint_keep.boolean", keep);
        return true;
    }

    /* Returns an ImageStack containing frames [beginSlice, endSlice)
     * from an ImageStack stack, indexed from zero */
    public ImageStack makeSubStack(ImageStack stack, int beginSlice, int endSlice) {
        ImageStack newStack = new ImageStack(stack.getWidth(), stack.getHeight(), stack.getColorModel());
        for(int i = beginSlice; i < endSlice; ++i) {
            newStack.addSlice(stack.getProcessor(i+1));
        }
        return newStack;
    }

    /* given an ImageStack ordered by time and then by channel, return an 
     * ImageStack ordered by channel and then by time */
    public ImageStack makeShuffled(ImageStack stack) {
        ImageStack newStack = new ImageStack(stack.getWidth(), stack.getHeight(), stack.getColorModel());
        for(int channel = 0; channel < nChannels; ++channel) {
            for(int i = channel; i < stack.getSize(); i += nChannels) {
                newStack.addSlice(stack.getProcessor(i+1));
            }
        }
        return newStack;
    }
}
