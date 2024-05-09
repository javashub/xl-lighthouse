package com.dtstep.lighthouse.standalone.rpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private Object response;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof com.dtstep.lighthouse.standalone.rpc.RpcResponse){
            com.dtstep.lighthouse.standalone.rpc.RpcResponse response = (com.dtstep.lighthouse.standalone.rpc.RpcResponse) msg;
            this.response = response.getResult();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    public Object getResponse() {
        return this.response;
    }
}
