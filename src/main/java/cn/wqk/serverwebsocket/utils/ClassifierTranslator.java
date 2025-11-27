package cn.wqk.serverwebsocket.utils;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.util.List;

// 直接实现 Translator 接口
public class ClassifierTranslator implements Translator<Image, Classifications> {

    private final int imageSize;
    private final List<String> synset; // 用于后处理的类别列表

    // 构造函数保持不变
    public ClassifierTranslator(int imageSize, List<String> synset) {
        this.imageSize = imageSize;
        this.synset = synset;
    }

    // 1. 覆写 processInput：处理图片预处理
    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {

        // 1. Resize/Crop 到 PyTorch 所需的尺寸
        NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
        array = NDImageUtils.resize(array, 256, 256);
        array = NDImageUtils.centerCrop(array, imageSize, imageSize);

        // 2. 将 HWC (高宽通道) 转换为 CHW (通道高宽)
        // 并将像素值归一化到 0-1 范围 (PyTorch 常用)
        array = array.div(255.0f).transpose(2, 0, 1);

        // 3. Normalize：使用 PyTorch ImageNet 的 Mean/Std
        // 注意：这里需要手动实现 Normalize 逻辑
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std = {0.229f, 0.224f, 0.225f};

        for (int i = 0; i < array.size(0); i++) {
            array.get(i).sub(mean[i]).div(std[i]);
        }

        // 4. 返回 NDList
        return new NDList(array);
    }

    // 2. 覆写 processOutput：处理模型输出
    @Override
    public Classifications processOutput(TranslatorContext ctx, NDList list) {
        NDArray output = list.singletonOrThrow();

        // 确保输出是 Float32，并应用 Softmax
        if (output.getDataType() != DataType.FLOAT32) {
            output = output.toType(DataType.FLOAT32, false);
        }

        // 应用 Softmax 获取概率分布
        NDArray probabilities = output.softmax(0);

        // 返回 Classifications 对象
        return new Classifications(synset, probabilities);
    }
}