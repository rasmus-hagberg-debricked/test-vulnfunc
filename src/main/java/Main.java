import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String data;
        if (args.length == 0) {
            data = "example data";
        } else {
            data = String.join(" ", args);
        }
        byte[] original = data.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compress(original);
        byte[] decompressed = decompress(compressed);

        System.out.println("Original: " + new String(original));
        System.out.println("Compressed: " + new String(compressed));
        System.out.println("Decompressed: " + new String(decompressed));
    }

    private static byte[] compress(byte[] data) {
        JdkZlibEncoder enc = new JdkZlibEncoder();
        EmbeddedChannel channel = new EmbeddedChannel(enc);
        channel.writeOutbound(Unpooled.wrappedBuffer(data));
        channel.finish();

        int outputSize = 0;
        ByteBuf o;
        List<ByteBuf> outbound = new ArrayList<>();
        while ((o = channel.readOutbound()) != null) {
            outbound.add(o);
            outputSize += o.readableBytes();
        }

        byte[] output = new byte[outputSize];
        int readCount = 0;
        for (ByteBuf b : outbound) {
            int readableBytes = b.readableBytes();
            b.readBytes(output, readCount, readableBytes);
            b.release();
            readCount += readableBytes;
        }

        return output;
    }

    private static byte[] decompress(byte[] data) {
        JdkZlibDecoder dec = new JdkZlibDecoder();
        EmbeddedChannel channel = new EmbeddedChannel(dec);
        channel.writeInbound(Unpooled.wrappedBuffer(data));
        channel.finish();

        int outputSize = 0;
        ByteBuf o;
        List<ByteBuf> inbound = new ArrayList<>();
        while ((o = channel.readInbound()) != null) {
            inbound.add(o);
            outputSize += o.readableBytes();
        }

        byte[] output = new byte[outputSize];
        int readCount = 0;
        for (ByteBuf b : inbound) {
            int readableBytes = b.readableBytes();
            b.readBytes(output, readCount, readableBytes);
            b.release();
            readCount += readableBytes;
        }

        return output;
    }

}

// CVE-2019-20444 allows invalid syntax
// fixed in https://github.com/netty/netty/pull/9871/files
// affects io.netty.handler.codec.http.HttpObjectDecoder.splitHeader(AppendableCharSequence)
// not used

// CVE-2019-20445 multiple Content-Length
// fixed in https://github.com/netty/netty/commit/cc37c204f1b5008c8b123f3710821703b52129d8
// affects io.netty.handler.codec.http.HttpObjectDecoder.readHeaders(ByteBuf), verifyAndAddHeader(HttpHeaders, boolean), verifyContentLengthHeader(boolean) and a test file
// not used

// CVE-2020-11612 no memory limit on allocation in (de)compression
// fixed in https://github.com/netty/netty/pull/9924/files
// affects io.netty.handler.codec.compression.JdkZlibDecoder.<init>(), <init>(int), <init>(ZlibWrapper),
// <init>(ZlibWrapper, int), <init>(byte[]), <init>(byte[], int), decode(ChannelHandlerContext, ByteBuf, List<Object>, decompressionBufferExhausted(ByteBuf)
// similar methods in JZlibDecoder, ZlibDecoder
// used

// CVE-2021-21295 todo analyse
