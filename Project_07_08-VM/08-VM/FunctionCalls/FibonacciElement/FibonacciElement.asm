@256
D=A
@SP
M=D
//call Sys.init 0
@returnLabelIdx0
D=A
@SP
A=M
M=D
@SP
M=M+1
@LCL
D=M
@SP
A=M
M=D
@SP
M=M+1
@ARG
D=M
@SP
A=M
M=D
@SP
M=M+1
@THIS
D=M
@SP
A=M
M=D
@SP
M=M+1
@THAT
D=M
@SP
A=M
M=D
@SP
M=M+1
//ARG=SP-5-nArgs
@SP
D=M
@5
D=D-A
@0
D=D-A
@ARG
M=D
@SP
D=M
@LCL
M=D
//goto Sys.init
@Sys.init
0;JMP
(returnLabelIdx0)
//function Main.fibonacci 
(Main.fibonacci)
//push nVars 0s to @LCL using SP=LCL
@SP
D=M
@LCL
M=D
@0
D=A
(loopIdx1)
@endLoopIdx1
D;JEQ
@SP
A=M
M=0
@SP
M=M+1
D=D-1
@loopIdx1
0;JMP
(endLoopIdx1)
//push argument 0
@0
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1
//push constant 2
@2
D=A
@SP
A=M
M=D
@SP
M=M+1
//lt
@SP
AM=M-1
D=M
@SP
AM=M-1
A=M
D=A-D
@trueLabelIdx2
D;JLT
D=0
@falseExitLabelIdx2
0;JMP
(trueLabelIdx2)
D=-1
(falseExitLabelIdx2)
@SP
A=M
M=D
@SP
M=M+1
//if-goto IF_TRUE
@SP
AM=M-1
D=M
@IF_TRUE
D;JNE
//goto IF_FALSE
@IF_FALSE
0;JMP
(IF_TRUE)
//push argument 0
@0
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1
//return
//calcReturnAddrPointer
@LCL
D=M
@5
A=D-A
D=M
@R14
M=D
//pop returnValue in @Arg
@SP
AM=M-1
D=M
@ARG
A=M
M=D
//Set SP
D=A
@SP
M=D+1
//Set THAT
@LCL
AM=M-1
D=M
@THAT
M=D
//Set THIS
@LCL
AM=M-1
D=M
@THIS
M=D
//Set ARG
@LCL
AM=M-1
D=M
@ARG
M=D
//Set LCL
@LCL
A=M-1
D=M
@LCL
M=D
//goto returnAddr
@R14
A=M
0;JMP
(IF_FALSE)
//push argument 0
@0
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1
//push constant 2
@2
D=A
@SP
A=M
M=D
@SP
M=M+1
//sub
@SP
AM=M-1
D=M
@SP
AM=M-1
A=M
D=A-D
@SP
A=M
M=D
@SP
M=M+1
//call Main.fibonacci 1
@returnLabelIdx3
D=A
@SP
A=M
M=D
@SP
M=M+1
@LCL
D=M
@SP
A=M
M=D
@SP
M=M+1
@ARG
D=M
@SP
A=M
M=D
@SP
M=M+1
@THIS
D=M
@SP
A=M
M=D
@SP
M=M+1
@THAT
D=M
@SP
A=M
M=D
@SP
M=M+1
//ARG=SP-5-nArgs
@SP
D=M
@5
D=D-A
@1
D=D-A
@ARG
M=D
@SP
D=M
@LCL
M=D
//goto Main.fibonacci
@Main.fibonacci
0;JMP
(returnLabelIdx3)
//push argument 0
@0
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1
//push constant 1
@1
D=A
@SP
A=M
M=D
@SP
M=M+1
//sub
@SP
AM=M-1
D=M
@SP
AM=M-1
A=M
D=A-D
@SP
A=M
M=D
@SP
M=M+1
//call Main.fibonacci 1
@returnLabelIdx4
D=A
@SP
A=M
M=D
@SP
M=M+1
@LCL
D=M
@SP
A=M
M=D
@SP
M=M+1
@ARG
D=M
@SP
A=M
M=D
@SP
M=M+1
@THIS
D=M
@SP
A=M
M=D
@SP
M=M+1
@THAT
D=M
@SP
A=M
M=D
@SP
M=M+1
//ARG=SP-5-nArgs
@SP
D=M
@5
D=D-A
@1
D=D-A
@ARG
M=D
@SP
D=M
@LCL
M=D
//goto Main.fibonacci
@Main.fibonacci
0;JMP
(returnLabelIdx4)
//add
@SP
AM=M-1
D=M
@SP
AM=M-1
A=M
D=D+A
@SP
A=M
M=D
@SP
M=M+1
//return
//calcReturnAddrPointer
@LCL
D=M
@5
A=D-A
D=M
@R14
M=D
//pop returnValue in @Arg
@SP
AM=M-1
D=M
@ARG
A=M
M=D
//Set SP
D=A
@SP
M=D+1
//Set THAT
@LCL
AM=M-1
D=M
@THAT
M=D
//Set THIS
@LCL
AM=M-1
D=M
@THIS
M=D
//Set ARG
@LCL
AM=M-1
D=M
@ARG
M=D
//Set LCL
@LCL
A=M-1
D=M
@LCL
M=D
//goto returnAddr
@R14
A=M
0;JMP
//function Sys.init 
(Sys.init)
//push nVars 0s to @LCL using SP=LCL
@SP
D=M
@LCL
M=D
@0
D=A
(loopIdx5)
@endLoopIdx5
D;JEQ
@SP
A=M
M=0
@SP
M=M+1
D=D-1
@loopIdx5
0;JMP
(endLoopIdx5)
//push constant 4
@4
D=A
@SP
A=M
M=D
@SP
M=M+1
//call Main.fibonacci 1
@returnLabelIdx6
D=A
@SP
A=M
M=D
@SP
M=M+1
@LCL
D=M
@SP
A=M
M=D
@SP
M=M+1
@ARG
D=M
@SP
A=M
M=D
@SP
M=M+1
@THIS
D=M
@SP
A=M
M=D
@SP
M=M+1
@THAT
D=M
@SP
A=M
M=D
@SP
M=M+1
//ARG=SP-5-nArgs
@SP
D=M
@5
D=D-A
@1
D=D-A
@ARG
M=D
@SP
D=M
@LCL
M=D
//goto Main.fibonacci
@Main.fibonacci
0;JMP
(returnLabelIdx6)
(WHILE)
//goto WHILE
@WHILE
0;JMP